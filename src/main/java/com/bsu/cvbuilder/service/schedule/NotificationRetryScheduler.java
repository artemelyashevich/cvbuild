package com.bsu.cvbuilder.service.schedule;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.entity.Notification;
import com.bsu.cvbuilder.domain.entity.NotificationStatus;
import com.bsu.cvbuilder.repository.NotificationRepository;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.util.CacheUtil;
import com.bsu.cvbuilder.util.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

import static com.bsu.cvbuilder.util.CacheUtil.NOTIFICATION_DELAYED_KEY;
import static com.bsu.cvbuilder.util.CacheUtil.NOTIFICATION_PROCESSING;
import static com.bsu.cvbuilder.util.JsonHelper.fromJson;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedRate = 3000)
    public void job() {

        String notification = redisTemplate.opsForList()
                .rightPopAndLeftPush(CacheUtil.NOTIFICATION_RETRY_KEY, CacheUtil.NOTIFICATION_PROCESSING);

        if (notification == null) return;

        NotificationDto dto = (NotificationDto) fromJson(notification, NotificationDto.class);

        try {
            notificationService.sendInternal(dto);
            notificationRepository.findByUuid(dto.getId())
                    .ifPresent(n -> {
                        n.setStatus(NotificationStatus.SUCCESS);
                        notificationRepository.save(n);
                    });
            redisTemplate.opsForList()
                    .remove(CacheUtil.NOTIFICATION_PROCESSING, 1, notification);
        } catch (Exception e) {

            int retry = dto.getRetryCount() + 1;

            if (retry >= 5) {
                moveToDLQ(notification);
            } else {
                dto.setRetryCount(retry);
                requeueWithDelay(dto);
            }
            redisTemplate.opsForList()
                    .remove(NOTIFICATION_PROCESSING, 1, notification);
        }
    }

    private void moveToDLQ(String notificationJson) {

        NotificationDto dto = (NotificationDto) fromJson(notificationJson, NotificationDto.class);

        Notification entity = Notification.builder()
                .engine(dto.getEngine())
                .receiver(dto.getReceiver())
                .content(dto.getParameters())
                .status(NotificationStatus.FAILURE)
                .build();

        notificationRepository.save(entity);

        log.error("Moved to DLQ: {}", dto.getReceiver());
    }

    private void requeueWithDelay(NotificationDto dto) {

        long delayMillis = calculateBackoff(dto.getRetryCount());
        long score = System.currentTimeMillis() + delayMillis;

        String json = JsonHelper.toJson(dto);

        redisTemplate.opsForZSet()
                .add(NOTIFICATION_DELAYED_KEY, json, score);

        log.info("Notification requeued with delay {} ms, retry={}",
                delayMillis, dto.getRetryCount());
    }

    private long calculateBackoff(int retry) {
        long base = switch (retry) {
            case 1 -> 5_000;
            case 2 -> 30_000;
            case 3 -> 120_000;
            case 4 -> 600_000;
            default -> 1_800_000;
        };

        long jitter = ThreadLocalRandom.current().nextLong(0, 3000);

        return base + jitter;
    }
}
