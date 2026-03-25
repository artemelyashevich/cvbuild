package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.NotificationStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationStrategyImpl implements NotificationStrategy {

    private final ApplicationProperties applicationProperties;
    private final RestTemplate restTemplate;

    private String url;

    @PostConstruct
    public void init() {
        this.url = applicationProperties.getTelegram().getUrl().formatted(
                applicationProperties.getTelegram().getToken(),
                applicationProperties.getTelegram().getChatId()
        );
    }

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        try {
            String finalUrl = UriComponentsBuilder
                    .fromUriString(url)
                    .queryParamIfPresent("text", Optional.of(notificationDto.getParameters().get("message")))
                    .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new AppException("retry", 5000);
            }
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.TELEGRAM;
    }
}
