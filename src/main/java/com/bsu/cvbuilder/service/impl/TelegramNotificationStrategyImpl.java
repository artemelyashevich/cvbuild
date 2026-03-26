package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.NotificationStrategy;
import com.bsu.cvbuilder.util.JsonHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
                applicationProperties.getTelegram().getToken()
        );
    }

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("chat_id", applicationProperties.getTelegram().getChatId());
            params.add("text", (String) notificationDto.getParameters().get("message"));
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new AppException("retry", 5000);
            }
        } catch (HttpClientErrorException e) {
            log.error(e.getMessage());
            throw new AppException("retry", 5000);
        }
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.TELEGRAM;
    }

    private record TelegramBody (
            String chatId, String text
    ){}
}
