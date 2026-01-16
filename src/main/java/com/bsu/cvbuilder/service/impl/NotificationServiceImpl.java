package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final Map<String, NotificationStrategy> notificationStrategyMap;

    //@Async
    @Override
    public void sendNotification(NotificationDto notificationDto) {
        NotificationStrategy strategy = notificationStrategyMap.get(notificationDto.getEngine().name().toLowerCase());
        if (strategy == null) {
            throw new AppException("Attempting find unexisting notification strategy with name: %s".formatted(notificationDto.getEngine().name()), 500);
        }
        log.info("Sending notification to {}", notificationDto.getEngine().name().toLowerCase());
        strategy.sendNotification(notificationDto);
        log.info("Sent notification to {}", notificationDto.getEngine().name().toLowerCase());
    }
}
