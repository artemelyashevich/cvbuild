package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.annotation.limit.Limited;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final BeanOutputConverter<Resume> converter = new BeanOutputConverter<>(Resume.class);

    private final AiService aiService;
    private final ChatService chatService;
    private final MongoTemplate mongoTemplate;

    @Override
    @Transactional(readOnly = true)
    public Page<Resume> findAll(Pageable pageable) {
        log.debug("Fetching page of resumes: {}", pageable);
        Query query = new Query().with(pageable);

        List<Resume> list = mongoTemplate.find(query, Resume.class);

        return PageableExecutionUtils.getPage(
                list,
                pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Resume.class)
        );
    }

    @Override
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
        Resume resume = findById(resumeId);

        resume.setBlocks(updateRequest.blocks());

        return mongoTemplate.save(resume);
    }

    private Resume generateAndSave(UUID chatId) {
        AiChat chat = chatService.getChatById(chatId);

        if (!chat.isFinished()) {
            throw new AppException("Failed to convert not finished chat with id: " + chatId, 404);
        }

        String contextHistory = chat.getMessages().stream()
                .map(m -> String.format("%s: %s", m.getRole(), m.getContent()))
                .collect(Collectors.joining("\n"));

        String promptWithFormat = contextHistory + "\n\n" + converter.getFormat();

        try {
            log.debug("Calling AI Extractor for chat {}", chatId);
            var responseSpec = aiService.callExtractor(promptWithFormat, chatId);

            Resume extractedResume = responseSpec.entity(converter);

            if (extractedResume == null) {
                throw new AppException("AI returned empty resume data", 500);
            }

            extractedResume.setChatId(chatId.toString());
            Resume saved = mongoTemplate.save(extractedResume);

            log.info("Successfully generated and saved resume for chat {}", chatId);
            return saved;

        } catch (Exception e) {
            log.error("Failed to generate resume for chat {}: {}", chatId, e.getMessage());
            throw new AppException("Failed to generate resume via AI. Please try to chat more.", e, 500);
        }
    }
}