package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.AiTemplateMessage;
import com.bsu.cvbuilder.entity.chat.MessageRole;
import com.bsu.cvbuilder.entity.resume.ResumeData;
import com.bsu.cvbuilder.entity.chat.AiChat;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.ResumeRepository;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResumeServiceImpl implements ResumeService {

    private final ChatClient chatClient;
    private final ChatService chatService;
    private final ResumeRepository resumeRepository;

    private final BeanOutputConverter<ResumeData> converter = new BeanOutputConverter<>(ResumeData.class);

    public ResumeServiceImpl(ChatClient.Builder builder, ChatService chatService, ResumeRepository resumeRepository) {
        this.chatClient = builder
                .build();
        this.chatService = chatService;
        this.resumeRepository = resumeRepository;
    }

    @Override
    public ResumeData extract(UUID chatId) {
        return resumeRepository.findByChatId(chatId.toString())
                .orElseGet(() -> generateAndSave(chatId));
    }

    private ResumeData generateAndSave(UUID chatId) {
        log.info("Starting AI extraction for chatId={}", chatId);
        AiChat history = chatService.getChatById(chatId);

        String cleanedHistory = history.getMessages().stream()
                .filter(m -> !MessageRole.ASSISTANT.equals(m.getRole()))
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        try {
            ResumeData data = chatClient.prompt()
                    .user(u -> u.text(AiTemplateMessage.SYSTEM_EXTRACTOR.getMessage().formatted(cleanedHistory)))
                    .options(OllamaOptions.builder()
                            .format("json")
                            .temperature(0.2)
                            .numPredict(2000)
                            .build())
                    .call()
                    .entity(converter);

            if (data != null) {
                data.setChatId(chatId.toString());
                return resumeRepository.save(data);
            }
        } catch (Exception e) {
            log.error("Failed to extract resume for chat {}: {}", chatId, e.getMessage());
            throw new AppException("AI Generation failed", e, 500);
        }
        return null;
    }
}