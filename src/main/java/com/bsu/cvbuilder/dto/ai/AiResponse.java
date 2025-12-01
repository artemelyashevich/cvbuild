package com.bsu.cvbuilder.dto.ai;

import lombok.Builder;

@Builder
public record AiResponse(
        String message,
        boolean agree
) {
}
