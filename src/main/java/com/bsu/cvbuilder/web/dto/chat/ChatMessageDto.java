package com.bsu.cvbuilder.web.dto.chat;

import com.bsu.cvbuilder.domain.entity.MessageRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageDto(
        UUID id,
        MessageRole role,
        String content,
        LocalDateTime timestamp
) {
}
