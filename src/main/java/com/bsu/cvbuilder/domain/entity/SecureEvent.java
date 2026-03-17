package com.bsu.cvbuilder.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum SecureEvent {
    setPassword(Duration.ofDays(30)),
    resetPassword(Duration.ofDays(1)),
    enable2fa(Duration.ofMinutes(10)),
    verifyEmail(Duration.ofDays(7)),
    changeNotificationEngine(Duration.ofMinutes(5)),
    agreement(Duration.ofMinutes(10)),
    deleteAccount(Duration.ofDays(1));

    private final Duration duration;
}
