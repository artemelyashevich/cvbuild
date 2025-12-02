package com.bsu.cvbuilder.web.mapper.impl;

import com.bsu.cvbuilder.entity.chat.ChatMessage;
import com.bsu.cvbuilder.entity.chat.ChatSession;
import com.bsu.cvbuilder.web.dto.chat.ChatResponseDto;
import com.bsu.cvbuilder.web.mapper.Mappable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ChatDtoMapper implements Mappable<ChatSession, ChatResponseDto> {
    @Override
    public ChatSession toEntity(ChatResponseDto dto) {
        return new ChatSession();
    }

    @Override
    public ChatResponseDto toDto(ChatSession dto) {
        return ChatResponseDto.builder()
                .chatMessages(dto.getMessageHistory().stream()
                        .sorted(Comparator.comparing(ChatMessage::getQuestionId))
                        .sorted(Comparator.comparing(ChatMessage::getRole))
                        .toList())
                .build();
    }

    @Override
    public List<ChatResponseDto> toDtoList(List<ChatSession> dtoList) {
        return dtoList.stream()
                .map(this::toDto)
                .toList();
    }
}
