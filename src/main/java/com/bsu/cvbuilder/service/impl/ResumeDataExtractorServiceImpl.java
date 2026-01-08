package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.AiTemplateMessage;
import com.bsu.cvbuilder.domain.ResumeData;
import com.bsu.cvbuilder.entity.chat.AiChat;
import com.bsu.cvbuilder.entity.chat.AiMessage;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeDataExtractorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ResumeDataExtractorServiceImpl implements ResumeDataExtractorService {

    private final ChatClient chatClient;
    private final ChatService chatService;

    public ResumeDataExtractorServiceImpl(ChatClient.Builder builder, ChatService chatService) {
        this.chatClient = builder.build();
        this.chatService = chatService;
    }

    @Override
    public ResumeData extract(UUID chatId) {
        log.info("Extracting resume data for chatId={}", chatId);
        AiChat history = chatService.getChatById(chatId);

        StringBuilder sb = new StringBuilder();

        history.getMessages().forEach(message -> sb
                .append(message.getContent())
                .append("\n---ROLE: ")
                .append(message.getRole())
                .append("---\n")
        );

        var converter = new BeanOutputConverter<>(ResumeData.class);

        ResumeData data = chatClient.prompt()
                .user(AiTemplateMessage.SYSTEM_EXTRACTOR.getMessage().formatted(sb.toString()))
                .options(OllamaOptions.builder().format("json").build())
                .call()
                .entity(converter);

        log.info("Extracted resume data for chatId={}", chatId);
        return data;
    }
}

