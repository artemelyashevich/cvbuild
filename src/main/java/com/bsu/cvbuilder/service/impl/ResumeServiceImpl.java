package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.chat.MessageRole;
import com.bsu.cvbuilder.entity.resume.Resume;
import com.bsu.cvbuilder.entity.chat.AiChat;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final AiService aiService;
    private final ChatService chatService;
    private final MongoTemplate mongoTemplate;

    private final BeanOutputConverter<Resume> converter = new BeanOutputConverter<>(Resume.class);

    @Override
    public Resume findByChatId(UUID chatId) {
        var resume = mongoTemplate.findOne(new Query(Criteria.where("chatId").is(chatId)), Resume.class);
        if (resume == null) {
            return generateAndSave(chatId);
        }
        return resume;
    }

    @Override
    public Resume findById(String id) {
        var resume = mongoTemplate.findById(id, Resume.class);
        if (resume == null) {
            throw new AppException("There are no resume with such id: %s".formatted(id), 404);
        }
        return resume;
    }

    private Resume generateAndSave(UUID chatId) {
        log.info("Starting AI extraction for chatId={}", chatId);
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
            return resume;
        } catch (Exception e) {
            log.error("Failed to extract resume for chat {}: {}", chatId, e.getMessage());
            throw new AppException("AI Generation failed", e, 500);
        }
    }
}