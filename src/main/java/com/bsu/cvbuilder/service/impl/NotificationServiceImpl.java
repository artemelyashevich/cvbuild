package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.domain.entity.history.Notification;
import com.bsu.cvbuilder.repository.NotificationRepository;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.NotificationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final Map<NotificationEngine, NotificationStrategy> notificationStrategyMap;

    public NotificationServiceImpl(NotificationRepository notificationRepository, List<NotificationStrategy> strategies) {
        this.notificationRepository = notificationRepository;
        this.notificationStrategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        NotificationStrategy::getSupportedEngine,
                        s -> s
                ));
    }

    @Async
    @Override
    public void sendNotification(NotificationDto notificationDto) {
        if (notificationDto == null || notificationDto.getEngine() == null) {
            log.error("Notification attempt with null DTO or Engine");
            return;
        }

        NotificationStrategy strategy = notificationStrategyMap.get(notificationDto.getEngine());

        if (strategy == null) {
            log.error("No notification strategy found for engine: {}", notificationDto.getEngine());
            return;
        }

        try {
            log.info("Sending {} notification to {}",
                    notificationDto.getEngine(),
                    notificationDto.getReceiver());

            strategy.sendNotification(notificationDto);

            notificationRepository.save(Notification.builder()
                            .engine(notificationDto.getEngine())
                            .receiver(notificationDto.getReceiver())
                            .content(notificationDto.getParameters())
                    .build());

            log.info("Successfully sent notification to {}", notificationDto.getReceiver());
        } catch (Exception e) {
            log.error("Failed to send notification to {}. Error: {}",
                    notificationDto.getReceiver(), e.getMessage());
        }
    }
}
