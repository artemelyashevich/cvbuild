package com.bsu.cvbuilder.domain.dto.ai;

import com.bsu.cvbuilder.domain.entity.chat.ChatState;

import java.util.UUID;

public record AiRequest(
        String message,
        UUID chatId,
        ChatState chatState
) {
}
