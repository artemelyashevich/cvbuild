package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.AiTemplateMessage;
import com.bsu.cvbuilder.entity.chat.MessageRole;
import com.bsu.cvbuilder.entity.resume.Resume;
import com.bsu.cvbuilder.entity.chat.AiChat;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResumeServiceImpl implements ResumeService {

    private final ChatClient chatClient;
    private final ChatService chatService;
    private final MongoTemplate mongoTemplate;

    private final BeanOutputConverter<Resume> converter = new BeanOutputConverter<>(Resume.class);

    public ResumeServiceImpl(ChatClient.Builder builder, ChatService chatService, MongoTemplate mongoTemplate) {
        this.chatClient = builder
                .build();
        this.chatService = chatService;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Resume extract(UUID chatId) {
        var resume = mongoTemplate.findOne(new Query(Criteria.where("chatId").is(chatId)), Resume.class);
        if (resume == null) {
            return generateAndSave(chatId);
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
            var data = chatClient.prompt()
                    .user(u -> u.text(AiTemplateMessage.SYSTEM_EXTRACTOR.getMessage().formatted(cleanedHistory)))
                    .options(OllamaOptions.builder()
                            .format("json")
                            .temperature(0.2)
                            .numPredict(2000)
                            .build())
                    .call();
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