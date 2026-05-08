package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.annotation.limit.Limited;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import com.bsu.cvbuilder.domain.dto.notification.WsType;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.CreateResumeEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.*;
import com.bsu.cvbuilder.util.LockUtil;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final BeanOutputConverter<Resume> converter = new BeanOutputConverter<>(Resume.class);

    private final AiService aiService;
    private final ChatService chatService;
    private final MongoTemplate mongoTemplate;
    private final LockService lockService;
    private final SecurityService securityService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationContext applicationContext;
    private final JobParserService jobParserService;

    @Qualifier("taskFlowExecutor")
    private final Executor executor;

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
    @Cacheable(value = "resume:chatId:", key = "#chatId")
    @Limited(value = LimitType.RESUME_GENERATE, capacity = 5)
    public Resume findByChatId(UUID chatId) {
        log.debug("Finding resume for chat: {}", chatId);

        String chatIdStr = chatId.toString();
        Resume resume = mongoTemplate.findOne(
                Query.query(Criteria.where("chatId").is(chatIdStr)),
                Resume.class
        );

        if (resume == null) {
            log.info("Resume not found for chat {}, triggering AI generation", chatId);
            return generateAndSave(chatId);
        }

        return resume;
    }

    @Override
    public Resume findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, Resume.class))
                .orElseThrow(() -> new AppException("Resume not found with id: " + id, 404));
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

    @Override
    public void ats(String resumeId, String url) {
        AtomicReference<UserProfile> user = new AtomicReference<>(securityService.findCurrentUser());
        log.debug("ATS for resume: {}", resumeId);
        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.WS)
                .parameters(Map.of("message", "Резюме успешно отправлено в обработку!", "status", WsType.SUCCESS))
                .receiver(user.get().getLogin())
                .build());
        Resume byId = findById(resumeId);
        String parse = jobParserService.parse(url);
        CompletableFuture.runAsync(() -> {
            ChatClient.CallResponseSpec jobSpec = aiService.callExpansion(parse);
            log.info("Job parsing for resume: {} {}", resumeId, jobSpec.content());
            applicationContext.getBean(AnalyzerServiceImpl.class).ats(byId, jobSpec.content());
        }, executor);
    }

    private Resume generateAndSave(UUID chatId) {
        AiChat chat = chatService.getChatById(chatId);
        UserProfile userProfile = securityService.findCurrentUser();

        if (!chat.isFinished()) {
            throw new AppException("Failed to convert not finished chat with id: " + chatId, 404);
        }

        String contextHistory = chat.getMessages().stream()
                .map(m -> String.format("%s: %s", m.getRole(), m.getContent()))
                .collect(Collectors.joining("\n"));

        String promptWithFormat = contextHistory + "\n\n" + converter.getFormat();

        return lockService.withLock(LockUtil.RESUME.formatted(chatId), () -> {
            Map<String, Object> params = new HashMap<>();
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
                    Resume saved = mongoTemplate.save(resume);

                    log.info("Successfully generated and saved resume for chat {}", chatId);
                    params.put("resumeId", saved.getId());
                    params.put("status", "success");
                    applicationEventPublisher.publishEvent(CreateResumeEvent.builder()
                            .userId(userProfile.getId())
                            .build());
                    notificationService.sendNotification(
                            NotificationDto.builder()
                                    .receiver(SecurityContextHolder.getContext().getAuthentication().getName())
                                    .engine(NotificationEngine.WS)
                                    .parameters(Map.of("message", "Резюме сгенерировано, проверьте email", "type", WsType.SUCCESS))
                                    .build()
                    );
                    return saved;
                });

            } catch (Exception e) {
                log.error("Failed to generate resume for chat {}: {}", chatId, e.getMessage());
                notificationService.sendNotification(
                        NotificationDto.builder()
                                .receiver(SecurityContextHolder.getContext().getAuthentication().getName())
                                .engine(NotificationEngine.WS)
                                .parameters(Map.of("message", "Произошла ошибка во время генерации резюме, попробуйте еще раз позже...", "type", WsType.ERROR))
                                .build()
                );
                throw new AppException("Failed to generate resume via AI. Please try to chat more.", e, 500);
            } finally {
                if (params.get("status").equals("success")) {
                    sendNotification(userProfile.getEmail(), params, "resume_success");
                } else {
                    sendNotification(userProfile.getEmail(), params, "resume_rejected");
                }
            }
        });
    }

    private void sendNotification(String email, Map<String, Object> params, String templateName) {
        notificationService.sendNotification(
                NotificationDto.builder()
                        .engine(NotificationEngine.EMAIL)
                        .receiver(email)
                        .parameters(params)
                        .templateName(templateName)
                        .build()
        );
    }
}