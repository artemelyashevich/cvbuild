package com.bsu.cvbuilder.domain.dto.ai;

import java.time.LocalDateTime;

public record TokenUsageDto(
        String plan,
        long promptTokens,
        long completionTokens,
        long used,
        long limit,
        long remaining,
        LocalDateTime periodStart,
        LocalDateTime resetAt
) {
}
