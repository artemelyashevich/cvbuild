package com.bsu.cvbuilder.domain.entity.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum SecureEvent {
    resetPassword(Duration.ofDays(1)),
    verifyEmail(Duration.ofDays(7)),
    changeNotificationEngine(Duration.ofMinutes(5));

    private final Duration duration;
}
