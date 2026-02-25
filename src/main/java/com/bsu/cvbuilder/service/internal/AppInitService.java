package com.bsu.cvbuilder.service.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppInitService {

    private final ApplicationProperties applicationProperties;
    private final CacheManager cacheManager;

    @PostConstruct
    public void init() {
        log.info(" --- Initializing Application ---");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info(" --- Shutdown Application ---");
            Logger logger = (Logger) LoggerFactory.getLogger("com.bsu.cvbuilder");
            logger.setLevel(Level.OFF);
            log.info(" --- Clearing caches before shutdown ---");
            cacheManager.getCacheNames().forEach(cacheName ->
                    Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) { }
        }));

        var twilio = applicationProperties.getTwilio();
        if (twilio == null || twilio.getAccountSid() == null || twilio.getAuthToken() == null) {
            log.warn("Twilio properties not configured! Skipping Twilio initialization.");
        } else {
            Twilio.init(twilio.getAccountSid(), twilio.getAuthToken());
            log.info("Twilio initialized with SID: {}", twilio.getAccountSid());
        }
        log.info(" --- Application Initialization Complete ---");
    }
}