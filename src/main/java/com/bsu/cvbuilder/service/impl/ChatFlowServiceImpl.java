package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.dto.ChatRequestDto;
import com.bsu.cvbuilder.dto.ChatResponseDto;
import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.entity.chat.AiMessage;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatFlowService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ChatTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFlowServiceImpl implements ChatFlowService {

    private final AiService aiService;
    private final ChatTemplateService chatTemplateService;
    private final ChatService chatService;

    @Override
    public ChatResponseDto message(ChatRequestDto dto) {
        log.debug("--- CHAT FLOW SERVICE: STARTED - id: {} / isOptions: {} / options: {} ---",
                dto.chatId(),
                dto.isOptions(),
                dto.chatOptions()
        );

        return ChatResponseDto.builder()
                .isOption(false)
                .message(processMessage(dto))
                .build();
    }

    @Override
    public ChatResponseDto processAnswer(ChatRequestDto dto) {
        log.debug("--- CHAT FLOW SERVICE: Attempting to process answer: {} ---", dto.chatId());

        var chat = chatService.findByChatId(dto.chatId().toString());

        var template = chatTemplateService.findChatTemplate(chat.getTemplateId());

        var lastQuestion = chat.getAiQuestionsAnswers().lastEntry().getKey();


        return null;
    }

    public String processMessage(ChatRequestDto dto) {
        log.debug("--- CHAT FLOW SERVICE: PROCESSING - id: {} ---", dto.chatId());

        return aiService.call(AiRequestDto.builder()
                        .chatId(dto.chatId())
                        .aiRequestType(AiRequestDto.AiRequestType.get(dto.isUserAnswer()))
                        .content(dto.message())
                .build())
                .message();
    }
}
