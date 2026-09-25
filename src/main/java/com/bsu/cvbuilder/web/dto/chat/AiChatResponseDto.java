package com.bsu.cvbuilder.web.dto.chat;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AiChatResponseDto(
        UUID id,
        String userId,
        List<ChatMessageDto> messages,
        String templateId,
        boolean finished,
        ChatFlowStep chatFlowStep,
        Long version,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {
}
