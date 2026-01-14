package com.bsu.cvbuilder.domain.dto.ai;

import lombok.Builder;

@Builder
public record AiResponse(
        String message,
        boolean agree
) {
}
