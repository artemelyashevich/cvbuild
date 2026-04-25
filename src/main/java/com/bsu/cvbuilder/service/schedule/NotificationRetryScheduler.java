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
import static com.bsu.cvbuilder.util.JsonHelper.fromJson;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRetryScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedRate = 1000, scheduler = "notificationScheduleExecutor")
    public void job() {
        int batchSize = 50;
        long startTime = System.currentTimeMillis();
        int processedInBatch = 0;
        int successCount = 0;
        int retryCount = 0;
        int dlqCount = 0;

        for (int i = 0; i < batchSize; i++) {

            String notification = redisTemplate.opsForList()
                    .rightPopAndLeftPush(CacheUtil.NOTIFICATION_RETRY_KEY, CacheUtil.NOTIFICATION_PROCESSING);

            if (notification == null) {
                break;
            }
            log.debug("[NOTIFICATION-RETRY] Batch iteration {}/{}", i + 1, batchSize);

            processedInBatch++;
            log.info("[NOTIFICATION-RETRY] Processing notification from retry queue (item {}/{}): {}",
                    processedInBatch, batchSize,
                    notification.length() > 100 ? notification.substring(0, 100) + "..." : notification);

            try {
                NotificationDto dto = (NotificationDto) fromJson(notification, NotificationDto.class);

                if (dto == null) {
                    log.error("[NOTIFICATION-RETRY] Failed to deserialize notification, moving to DLQ: {}", notification);
                    moveToDLQ(notification);
                    dlqCount++;
                    continue;
                }

                log.info("[NOTIFICATION-RETRY] Sending notification to: {}", dto.getReceiver());
                long sendStartTime = System.currentTimeMillis();

                notificationService.sendInternal(dto);

                long sendTime = System.currentTimeMillis() - sendStartTime;
                log.debug("[NOTIFICATION-RETRY] Notification sent successfully in {}ms", sendTime);

                notificationRepository.findByUuid(dto.getId())
                        .ifPresentOrElse(n -> {
                            n.setStatus(NotificationStatus.SUCCESS);
                            notificationRepository.save(n);
                            log.debug("[NOTIFICATION-RETRY] Updated notification status to SUCCESS for ID: {}", dto.getId());
                        }, () -> log.warn("[NOTIFICATION-RETRY] Notification entity not found for ID: {}", dto.getId()));

                Long removed = redisTemplate.opsForList().remove(CacheUtil.NOTIFICATION_PROCESSING, 1, notification);
                log.info("[NOTIFICATION-RETRY] Notification sent successfully\n - ID: {}, Receiver: {}, Removed from processing queue: {}",
                        dto.getId(), dto.getReceiver(), removed != null && removed > 0);

                successCount++;

            } catch (Exception e) {
                log.error("[NOTIFICATION-RETRY] Error processing notification: {}", e.getMessage(), e);

                try {
                    NotificationDto dto = (NotificationDto) fromJson(notification, NotificationDto.class);

                    if (dto == null) {
                        log.error("[NOTIFICATION-RETRY] Cannot parse notification for retry handling, moving to DLQ");
                        moveToDLQ(notification);
                        dlqCount++;
                    } else {
                        int retry = dto.getRetryCount() + 1;
                        log.warn("[NOTIFICATION-RETRY] Notification failed\n - ID: {}, Receiver: {}, Current retry: {}, New retry: {}",
                                dto.getId(), dto.getReceiver(), dto.getRetryCount(), retry);

                        if (retry >= 5) {
                            log.error("[NOTIFICATION-RETRY] Max retries (5) exceeded for notification\n ID: {}, moving to DLQ", dto.getId());
                            moveToDLQ(notification);
                            dlqCount++;
                        } else {
                            dto.setRetryCount(retry);
                            requeueWithDelay(dto);
                            retryCount++;
                            log.info("[NOTIFICATION-RETRY] Notification requeued with delay\n - ID: {}, Retry count: {}/5",
                                    dto.getId(), retry);
                        }
                    }
                } catch (Exception parseError) {
                    log.error("[NOTIFICATION-RETRY] Critical error parsing notification for retry handling: {}", parseError.getMessage(), parseError);
                    moveToDLQ(notification);
                    dlqCount++;
                } finally {
                    Long removed = redisTemplate.opsForList().remove(CacheUtil.NOTIFICATION_PROCESSING, 1, notification);
                    log.debug("[NOTIFICATION-RETRY] Removed from processing queue: {}", removed != null && removed > 0);
                }
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        if (processedInBatch != 0) {
            log.info("[NOTIFICATION-RETRY] Job completed\n - Processed: {}, Success: {}, Retried: {}, DLQ: {}, Time: {}ms",
                    processedInBatch, successCount, retryCount, dlqCount, executionTime);
        }
    }

    private void moveToDLQ(String notificationJson) {
        log.warn("[NOTIFICATION-RETRY-DLQ] Moving notification to Dead Letter Queue");

        try {
            NotificationDto dto = (NotificationDto) fromJson(notificationJson, NotificationDto.class);

            if (dto == null) {
                log.error("[NOTIFICATION-RETRY-DLQ] Cannot parse notification, saving as raw JSON");
                Notification rawEntity = Notification.builder()
                        .content(notificationJson)
                        .status(NotificationStatus.FAILURE)
                        .build();
                notificationRepository.save(rawEntity);
                return;
            }

            log.error("[NOTIFICATION-RETRY-DLQ] Moving to DLQ\n - ID: {}, Receiver: {}, Engine: {}, Final retry count: {}",
                    dto.getId(), dto.getReceiver(), dto.getEngine(), dto.getRetryCount());

            Notification entity = Notification.builder()
                    .engine(dto.getEngine())
                    .receiver(dto.getReceiver())
                    .content(dto.getParameters())
                    .status(NotificationStatus.FAILURE)
                    .build();

            notificationRepository.save(entity);
            log.info("[NOTIFICATION-RETRY-DLQ] Successfully saved to DLQ with ID: {}", entity.getUuid());

        } catch (Exception e) {
            log.error("[NOTIFICATION-RETRY-DLQ] Failed to save to DLQ: {}", e.getMessage(), e);
        }
    }

    private void requeueWithDelay(NotificationDto dto) {
        long delayMillis = calculateBackoff(dto.getRetryCount());
        long score = System.currentTimeMillis() + delayMillis;

        log.info("[NOTIFICATION-RETRY] Requeuing with delay\n - ID: {}, Delay: {}ms, Scheduled time: {}, Retry count: {}",
                dto.getId(), delayMillis, score, dto.getRetryCount());

        String json = JsonHelper.toJson(dto);

        if (json == null) {
            log.error("[NOTIFICATION-RETRY] Failed to serialize notification for requeue - ID: {}", dto.getId());
            return;
        }

        redisTemplate.opsForZSet()
                .add(NOTIFICATION_DELAYED_KEY, json, score);

        log.debug("[NOTIFICATION-RETRY] Notification added to delayed queue - ID: {}, Score: {}, Queue size: {}",
                dto.getId(), score,
                redisTemplate.opsForZSet().size(NOTIFICATION_DELAYED_KEY));
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
        long delay = base + jitter;

        log.debug("[NOTIFICATION-RETRY] Calculated backoff\n - Retry: {}, Base delay: {}ms, Jitter: {}ms, Total: {}ms",
                retry, base, jitter, delay);

        return delay;
    }
}