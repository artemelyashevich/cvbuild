package com.bsu.cvbuilder.dto;

import com.bsu.cvbuilder.entity.chat.ChatState;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public record ChatRequestDto(
        String message,
        UUID chatId,
        ChatOptions chatOptions,
        boolean isOptions,
        ChatState chatState,
        String locale,
        boolean isUserAnswer
) {

    @Getter
    @Setter
    public static class ChatOptions {
        private ChatState chatState;
    }
}
