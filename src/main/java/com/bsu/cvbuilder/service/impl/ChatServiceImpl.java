package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.CreateChatEvent;
import com.bsu.cvbuilder.repository.AiChatRepository;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.LockService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.util.LockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final SecurityService securityService;
    private final AiChatRepository aiChatRepository;
    private final TransactionTemplate transactionTemplate;
    private final LockService lockService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public AiChat createAiChat(UUID chatId) {
        log.debug("Attempting to create a new AiChat with id {}", chatId);
        UserProfile user = securityService.findCurrentUser();
        AiChat aiChat = aiChatRepository.save(AiChat.builder()
                .id(chatId)
                .userId(user.getId())
                .build());
        log.info("Created AiChat with id {}", chatId);
        applicationEventPublisher.publishEvent(new CreateChatEvent(user.getId()));
        return aiChat;
    }

    @Override
    @Monitored(value = "finding_chat", context = "api")
    public AiChat getChatById(UUID chatId) {
        log.debug("Attempting to get AiChat with id {}", chatId);
        return transactionTemplate.execute(s -> {
            Optional<AiChat> byId = aiChatRepository.findById(chatId);
            return byId.orElseGet(() -> createAiChat(chatId));
        });
    }

    @Override
    @Monitored(value = "saving_chat", context = "api")
    public AiChat saveAiChat(AiChat aiChat) {
        log.debug("Attempting to save AiChat with id {}", aiChat.getId());
        return lockService.withLock(LockUtil.CHAT.formatted(aiChat.getId()), () -> aiChatRepository.save(aiChat));
    }

    @Override
    @Transactional
    public Page<AiChat> findAllByCurrentUser(Pageable pageable) {
        UserProfile user = securityService.findCurrentUser();
        log.debug("Attempting to find all AiChats by current user: {}", user.getLogin());
        Page<AiChat> aiChats = aiChatRepository.findAllByUserId(pageable, user.getId());
        log.info("Found all AiChats: {} by current user: {}", aiChats.getTotalElements(), user.getLogin());
        return aiChats;
    }

    @Override
    public void deleteAllByUserId(String id) {
        log.debug("Attempting to delete AiChat with user id {}", id);
        aiChatRepository.deleteByUserId(id);
        log.info("Deleted AiChats with user id {}", id);
    }
}
