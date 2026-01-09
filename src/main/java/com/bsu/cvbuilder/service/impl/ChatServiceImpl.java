package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.chat.AiChat;
import com.bsu.cvbuilder.repository.AiChatRepository;
import com.bsu.cvbuilder.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final AiChatRepository aiChatRepository;

    @Override
    public AiChat createAiChat(UUID chatId) {
        log.debug("Attempting to create a new AiChat with id {}", chatId);
        AiChat aiChat = aiChatRepository.save(AiChat.builder()
                        .id(chatId)
                .build());
        log.info("Created AiChat with id {}", chatId);
        return aiChat;
    }

    @Override
    public AiChat getChatById(UUID chatId) {
        log.debug("Attempting to get AiChat with id {}", chatId);
        Optional<AiChat> byId = aiChatRepository.findById(chatId);
        return byId.orElseGet(() -> createAiChat(chatId));
    }

    @Override
    public AiChat saveAiChat(AiChat aiChat) {
        log.debug("Attempting to save AiChat with id {}", aiChat.getId());
        AiChat newChat = aiChatRepository.save(aiChat);
        log.info("Saved AiChat with id {}", aiChat.getId());
        return newChat;
    }
}
