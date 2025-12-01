package com.bsu.cvbuilder.dto.ai;

import java.util.UUID;

public record AiRequestDto(
        UUID chatId,
        String content
) {
}
