package com.bsu.cvbuilder.dto.ai;

import com.bsu.cvbuilder.entity.chat.ChatState;

import java.util.UUID;

public record AiRequest(
        String message,
        UUID chatId,
        ChatState chatState
) {
}
