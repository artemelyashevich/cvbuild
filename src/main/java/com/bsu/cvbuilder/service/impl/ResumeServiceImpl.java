package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.chat.MessageRole;
import com.bsu.cvbuilder.domain.entity.resume.Resume;
import com.bsu.cvbuilder.domain.entity.chat.AiChat;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final AiService aiService;
    private final ChatService chatService;
    private final MongoTemplate mongoTemplate;
    private final ApplicationContext applicationContext;

    private final BeanOutputConverter<Resume> converter = new BeanOutputConverter<>(Resume.class);

    @Override
    public Page<Resume> findAll(Pageable pageable) {
        log.debug("Finding all resumes");
        Query query = new Query().with(pageable);
        List<Resume> all = mongoTemplate.find(query, Resume.class);
        Page<Resume> resumes = PageableExecutionUtils.getPage(
                all,
                pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Resume.class)
        );
        log.info("Find all resumes: {}", resumes.getTotalElements());
        return resumes;
    }

    @Override
    public Resume findByChatId(UUID chatId) {
        log.debug("Finding resume by chat id");
        var resume = mongoTemplate.findOne(new Query(Criteria.where("chatId").is(chatId)), Resume.class);
        if (resume == null) {
            log.debug("Unable to find resume by chat id");
            return generateAndSave(chatId);
        }
        log.info("Found resume by chat id");
        return resume;
    }

    @Override
    public Resume findById(String id) {
        log.debug("Attempting to find resume by id {}", id);
        var resume = mongoTemplate.findById(id, Resume.class);
        if (resume == null) {
            log.debug("Resume with id {} not found", id);
            throw new AppException("There are no resume with such id: %s".formatted(id), 404);
        }
        log.info("Resume with id {} found", id);
        return resume;
    }

    @Override
    public Resume update(String resumeId, UpdateResumeRequest updateResumeRequest) {
        log.debug("Attempting to update resume with id {}", resumeId);
        Resume resume = applicationContext.getBean(ResumeService.class).findById(resumeId);
        resume.setBlocks(updateResumeRequest.blocks());
        Resume updatedResume = mongoTemplate.save(resume);
        log.info("Resume with id {} updated", resumeId);
        return updatedResume;
    }

    private Resume generateAndSave(UUID chatId) {
        log.debug("Starting AI extraction for chatId={}", chatId);
        AiChat history = chatService.getChatById(chatId);

        String cleanedHistory = history.getMessages().stream()
                .filter(m -> !MessageRole.ASSISTANT.equals(m.getRole()))
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        try {
            var data = aiService.callExtractor(cleanedHistory, chatId);
            Resume resume = data.entity(converter);
            if (resume != null) {
                resume.setChatId(chatId.toString());
                mongoTemplate.save(resume);
            }
            log.info("Finished AI [FORM] extraction for chatId={}", chatId);
            return resume;
        } catch (Exception e) {
            log.error("Failed to extract resume for chat {}: {}", chatId, e.getMessage());
            throw new AppException("AI Generation failed", e, 500);
        }
    }
}