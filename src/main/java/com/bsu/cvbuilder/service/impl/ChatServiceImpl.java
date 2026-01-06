package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.chat.AiChat;
import com.bsu.cvbuilder.repository.AiChatRepository;
import com.bsu.cvbuilder.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final AiChatRepository aiChatRepository;

    @Override
    public AiChat createAiChat(UUID chatId) {
        return aiChatRepository.save(AiChat.builder()
                        .id(chatId)
                .build());
    }

    @Override
    public AiChat getChatById(UUID chatId) {
        Optional<AiChat> byId = aiChatRepository.findById(chatId);
        return byId.orElseGet(() -> createAiChat(chatId));
    }

    @Override
    public AiChat saveAiChat(AiChat aiChat) {
        return aiChatRepository.save(aiChat);
    }
}
