package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.CreateResumeEvent;
import com.bsu.cvbuilder.domain.event.ResumeNotificationEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.LimitService;
import com.bsu.cvbuilder.service.LockService;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.util.LockUtil;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final BeanOutputConverter<Resume> converter = new BeanOutputConverter<>(Resume.class);
    private static final int RESUME_GENERATE_CAPACITY = 5;

    private final AiService aiService;
    private final ChatService chatService;
    private final MongoTemplate mongoTemplate;
    private final LockService lockService;
    private final SecurityService securityService;
    private final LimitService limitService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Override
    public Resume save(Resume resume) {
        return mongoTemplate.save(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Resume> findAll(Pageable pageable) {
        UserProfile userProfile = securityService.findCurrentUser();

        log.debug("Fetching resumes for user: {}", userProfile.getId());

        Query query = new Query()
                .addCriteria(Criteria.where("resumeSettings.ownerId")
                        .is(userProfile.getId()))
                .with(pageable);

        List<Resume> list = mongoTemplate.find(query, Resume.class);

        return PageableExecutionUtils.getPage(
                list,
                pageable,
                () -> mongoTemplate.count(
                        Query.of(query),
                        Resume.class
                )
        );
    }

    @Override
    public Resume findByChatId(UUID chatId) {
        log.debug("Finding resume for chat: {}", chatId);
        AiChat chat = chatService.getOwnChat(chatId);

        Resume resume = mongoTemplate.findOne(
                Query.query(Criteria.where("chatId").is(chatId.toString())),
                Resume.class
        );

        if (resume == null) {
            log.info("Resume not found for chat {}, triggering AI generation", chatId);
            return generateAndSave(chat);
        }

        return resume;
    }

    @Override
    public Resume findById(String id) {
        return tryFindById(id)
                .orElseThrow(() -> new AppException("Resume not found with id: " + id, 404));
    }

    @Override
    public Optional<Resume> tryFindById(String id) {
        Resume resume = mongoTemplate.findById(id, Resume.class);
        if (resume == null) {
            return Optional.empty();
        }
        UserProfile user = securityService.findCurrentUser();
        if (!isOwner(resume, user.getId())) {
            throw new AppException("Access to resume %s is denied".formatted(id), 403);
        }
        return Optional.of(resume);
    }

    @Override
    @Transactional
    public Resume update(String resumeId, UpdateResumeRequest updateRequest) {
        log.debug("Updating resume: {}", resumeId);
        return lockService.withLock(LockUtil.RESUME.formatted(resumeId), () -> {
            Resume resume = findById(resumeId);

            resume.setBlocks(updateRequest.blocks());

            return mongoTemplate.save(resume);
        });
    }

    private boolean isOwner(Resume resume, String userId) {
        Resume.ResumeSettings settings = resume.getResumeSettings();
        if (settings != null && settings.getOwnerId() != null) {
            return Objects.equals(settings.getOwnerId(), userId);
        }
        // Resumes generated from a chat before ownerId was stored belong to the chat owner.
        return resume.getChatId() != null && chatService.isOwnedBy(UUID.fromString(resume.getChatId()), userId);
    }

    private Resume generateAndSave(AiChat chat) {
        UUID chatId = chat.getId();
        UserProfile userProfile = securityService.findCurrentUser();

        if (!chat.isFinished()) {
            throw new AppException("Failed to convert not finished chat with id: " + chatId, 404);
        }

        limitService.check(userProfile.getId(), LimitType.RESUME_GENERATE, RESUME_GENERATE_CAPACITY);

        String contextHistory = chat.getMessages().stream()
                .map(m -> String.format("%s: %s", m.getRole(), m.getContent()))
                .collect(Collectors.joining("\n"));

        String promptWithFormat = contextHistory + "\n\n" + converter.getFormat();

        return lockService.withLock(LockUtil.RESUME.formatted(chatId), () -> {
            try {
                log.debug("Calling AI Extractor for chat {}", chatId);
                var responseSpec = aiService.callExtractor(promptWithFormat, chatId);

                Resume extractedResume = responseSpec.entity(converter);

                ChatClient.CallResponseSpec expansionResumeSpec = aiService.callExpansion(extractedResume);

                Resume resume = expansionResumeSpec.entity(converter);

                return transactionTemplate.execute(s -> {
                    if (resume == null) {
                        throw new AppException("AI returned empty resume data", 500);
                    }

                    resume.setChatId(chatId.toString());
                    resume.setResumeSettings(withOwner(resume.getResumeSettings(), userProfile));
                    Resume saved = mongoTemplate.save(resume);

                    log.info("Successfully generated and saved resume for chat {}", chatId);
                    applicationEventPublisher.publishEvent(CreateResumeEvent.builder()
                            .userId(userProfile.getId())
                            .build());
                    applicationEventPublisher.publishEvent(new ResumeNotificationEvent(
                            userProfile.getLogin(), userProfile.getEmail(), saved.getId(), ResumeNotificationEvent.Kind.GENERATED));
                    return saved;
                });

            } catch (Exception e) {
                log.error("Failed to generate resume for chat {}: {}", chatId, e.getMessage());
                applicationEventPublisher.publishEvent(new ResumeNotificationEvent(
                        userProfile.getLogin(), userProfile.getEmail(), null, ResumeNotificationEvent.Kind.GENERATION_FAILED));
                throw new AppException("Failed to generate resume via AI. Please try to chat more.", e, 500);
            }
        });
    }

    private Resume.ResumeSettings withOwner(Resume.ResumeSettings settings, UserProfile owner) {
        Resume.ResumeSettings result = Optional.ofNullable(settings).orElseGet(Resume.ResumeSettings::new);
        result.setOwnerId(owner.getId());
        result.setOwnerLogin(owner.getLogin());
        return result;
    }
}
