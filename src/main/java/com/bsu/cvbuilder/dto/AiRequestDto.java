package com.bsu.cvbuilder.dto;

import java.util.UUID;

public record AiRequestDto(
        UUID chatId,
        String content
) {
}
