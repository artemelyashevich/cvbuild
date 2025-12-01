package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.dto.ai.AiResponse;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.RedisService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final MessageSource messageSource;
    private final SecurityService securityService;

    @Override
    public AiResponse call(AiRequestDto dto) {
        log.debug("Attempting call AI: {}", dto.chatId());

        var response = chatClient.prompt();
        return null;
    }

    private String createMessage(AiRequestDto dto) {
        var currentUser = securityService.findCurrentUser();
    }
}
