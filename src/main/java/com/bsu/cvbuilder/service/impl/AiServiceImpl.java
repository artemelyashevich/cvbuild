package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.dto.ai.AiResponse;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.AiChatRepository;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.RedisService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final RedisService redisService;
    private final MessageSource messageSource;
    private final SecurityService securityService;
    private final AiChatRepository aiChatRepository;

    @Override
    public AiResponse call(AiRequestDto dto) {
        log.debug("Attempting call AI: {}", dto.chatId());

        var response = chatClient.prompt(createMessage(dto)).call().content();
        var aiResponseBuilder = AiResponse.builder();

        if (response == null) {
            return new AiResponse(null, false);
        }

        if (dto.aiRequestType().equals(AiRequestDto.AiRequestType.QUESTION)) {
            aiResponseBuilder.agree(Integer.parseInt(response) > 5);
            return aiResponseBuilder.build();
        }
        aiResponseBuilder.message(response);
        return aiResponseBuilder.build();
    }

    private String createMessage(AiRequestDto dto) {
        var locale = redisService.getLocation(securityService.findCurrentUser().getEmail());
        var message = new StringBuilder();
        switch (dto.aiRequestType()) {
            case QUESTION -> {
                var helpPrompt = messageSource.getMessage("question", null, Locale.of(locale));
                message.append(helpPrompt);
            }
            case ANSWER -> {
                var helpPrompt = messageSource.getMessage("answer", null, Locale.of(locale));
                message.append(helpPrompt);
            }
            default -> throw new AppException("Unsupported message type", 500);
        }
        message.append(dto.content());
        return message.toString();
    }
}
