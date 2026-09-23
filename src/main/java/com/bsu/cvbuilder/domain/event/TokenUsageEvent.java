package com.bsu.cvbuilder.domain.event;

public record TokenUsageEvent(String userId, long promptTokens, long completionTokens) {
}
