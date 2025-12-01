package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.chat.AiChat;
import com.bsu.cvbuilder.repository.AiChatRepository;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ChatTemplateService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final AiChatRepository aiChatRepository;
    private final ChatTemplateService chatTemplateService;
    private final SecurityService securityService;

    @Override
    public void create() {
        log.debug("Attempting create new chat");

        var chat = AiChat.builder()
                .id(UUID.randomUUID().toString())
                .chatName("ResumeBuilder")
                .templateId(chatTemplateService.findChatTemplate().getId())
                .userId(securityService.findCurrentUser().getId())
                .build();

        aiChatRepository.save(chat);
    }

    @Override
    public AiChat findByChatId(String chatId) {
        return null;
    }

    @Override
    public void delete(String chatId) {

    }
}
