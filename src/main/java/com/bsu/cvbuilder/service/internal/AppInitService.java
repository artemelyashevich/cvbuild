package com.bsu.cvbuilder.service.internal;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.service.LockService;
import com.bsu.cvbuilder.service.NotificationService;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppInitService {

    private final ApplicationProperties applicationProperties;
    private final CacheManager cacheManager;
    private final LockService lockService;
    private final NotificationService notificationService;

    @PostConstruct
    public void init() {
        log.info(" --- Initializing Application ---");

        registerShutdownHook();
        if (applicationProperties.getTwilio().isEnabled()) {
            initTwilio();
        } else {
            log.info(" --- Twilio is disabled ---");
        }

        if (applicationProperties.getTelegram().getEnabled()) {
            notificationService.sendNotification(
                    NotificationDto.builder()
                            .receiver("")
                            .engine(NotificationEngine.TELEGRAM)
                            .parameters(Map.of("message", "Started!"))
                            .build()
            );
        } else {
            log.info(" --- Telegram is disabled ---");
        }

        log.info(" --- Application Initialization Complete ---");
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info(" --- Shutdown Application ---");
            try {
                cacheManager.getCacheNames().forEach(cacheName -> {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                        log.info("Cache '{}' cleared.", cacheName);
                    }
                });
            } catch (Exception e) {
                log.error("Error clearing caches on shutdown", e);
            }

            try {
                lockService.clear();
            } catch (Exception e) {
                log.error("Error releasing distributed locks on shutdown", e);
            }
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Shutdown wait interrupted", e);
            }
            notificationService.sendNotification(
                    NotificationDto.builder()
                            .receiver("")
                            .engine(NotificationEngine.TELEGRAM)
                            .parameters(Map.of("message", "Stopped!"))
                            .build());

            log.info(" --- Shutdown Complete ---");
        }));
    }

    private void initTwilio() {
        var twilio = applicationProperties.getTwilio();
        if (twilio == null || twilio.getAccountSid() == null || twilio.getAuthToken() == null) {
            log.warn("Twilio properties not configured! Skipping Twilio initialization.");
            return;
        }

        try {
            Twilio.init(twilio.getAccountSid(), twilio.getAuthToken());
            log.info("Twilio initialized with SID: {}", twilio.getAccountSid());
        } catch (Exception e) {
            log.error("Failed to initialize Twilio", e);
        }
    }
}