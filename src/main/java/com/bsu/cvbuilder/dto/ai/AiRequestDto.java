package com.bsu.cvbuilder.dto.ai;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AiRequestDto(
        UUID chatId,
        String content,
        AiRequestType aiRequestType
) {
    public enum AiRequestType {
        QUESTION, ANSWER;

        public static AiRequestType get(Boolean isUserAnswer) {
            return  isUserAnswer ? ANSWER : QUESTION;
        }
    }
}
