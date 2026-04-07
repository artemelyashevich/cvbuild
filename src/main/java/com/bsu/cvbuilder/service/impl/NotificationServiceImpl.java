package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.domain.entity.Notification;
import com.bsu.cvbuilder.domain.entity.NotificationStatus;
import com.bsu.cvbuilder.repository.NotificationRepository;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.NotificationStrategy;
import com.bsu.cvbuilder.util.CacheUtil;
import com.bsu.cvbuilder.util.JsonHelper;
import com.bsu.cvbuilder.util.MaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final Map<NotificationEngine, NotificationStrategy> notificationStrategyMap;

    public NotificationServiceImpl(NotificationRepository notificationRepository, RedisTemplate<String, String> redisTemplate, List<NotificationStrategy> strategies) {
        this.notificationRepository = notificationRepository;
        this.redisTemplate = redisTemplate;
        this.notificationStrategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        NotificationStrategy::getSupportedEngine,
                        s -> s
                ));
    }

    @Async("notificationExecutor")
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

        if (notificationDto.getReceiver() == null) {
            notificationDto.setReceiver("[internal]");
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
                    .status(NotificationStatus.SUCCESS)
                    .uuid(notificationDto.getId())
                    .build());

            log.info("Successfully sent notification to {}", MaskUtil.maskFirstFive(notificationDto.getReceiver()));
        } catch (Exception e) {
            log.error("Failed to send notification to {}. Error: {}",
                    notificationDto.getReceiver(), e.getMessage());
            notificationRepository.save(Notification.builder()
                    .engine(notificationDto.getEngine())
                    .receiver(notificationDto.getReceiver())
                    .content(notificationDto.getParameters())
                    .status(NotificationStatus.RETRY)
                    .uuid(notificationDto.getId())
                    .build());
            pushToRetryQueue(notificationDto);
        }
    }

    @Override
    public void sendInternal(NotificationDto dto) {
        NotificationStrategy strategy = notificationStrategyMap.get(dto.getEngine());

        if (strategy == null) {
            throw new IllegalStateException("No strategy for " + dto.getEngine());
        }

        strategy.sendNotification(dto);
    }

    private void pushToRetryQueue(NotificationDto notificationDto) {
        redisTemplate.opsForList().leftPush(CacheUtil.NOTIFICATION_RETRY_KEY, JsonHelper.toJson(notificationDto));
    }
}
