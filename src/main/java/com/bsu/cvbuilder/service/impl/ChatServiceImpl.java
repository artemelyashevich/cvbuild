package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.chat.AiChat;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.repository.AiChatRepository;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final SecurityService securityService;
    private final AiChatRepository aiChatRepository;

    @Override
    @Transactional
    public AiChat createAiChat(UUID chatId) {
        log.debug("Attempting to create a new AiChat with id {}", chatId);
        UserProfile user = securityService.findCurrentUser();
        AiChat aiChat = aiChatRepository.save(AiChat.builder()
                        .id(chatId)
                        .userId(user.getId())
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

    @Override
    @Transactional
    public Page<AiChat> findAllByCurrentUser(Pageable pageable) {
        log.debug("Attempting to find all AiChats by current user");
        UserProfile user = securityService.findCurrentUser();
        Page<AiChat> aiChats = aiChatRepository.findAllByUserId(pageable, user.getId());
        log.info("Found all AiChats by current user");
        return aiChats;
    }
}
