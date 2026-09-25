package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import com.bsu.cvbuilder.repository.NotificationRepository;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.schedule.NotificationRetryScheduler;
import com.bsu.cvbuilder.support.RedisTestSupport;
import com.bsu.cvbuilder.util.CacheUtil;
import com.bsu.cvbuilder.util.JsonHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Runs the job body against a real Redis (proxies such as ShedLock are not involved here).
 */
class NotificationRetrySchedulerTest {

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private NotificationService notificationService;
    private NotificationRepository notificationRepository;
    private NotificationRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        connectionFactory = RedisTestSupport.newConnectionFactory();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        clearQueues();
        notificationService = mock(NotificationService.class);
        notificationRepository = mock(NotificationRepository.class);
        when(notificationRepository.findByUuid(any())).thenReturn(Optional.empty());
        scheduler = new NotificationRetryScheduler(redisTemplate, notificationService, notificationRepository);
    }

    @AfterEach
    void tearDown() {
        clearQueues();
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("job: notifications left in processing by a crashed run are sent and the processing list is emptied")
    void job_LeftoversInProcessing_AreRecoveredAndSent() {
        NotificationDto dto = NotificationDto.builder()
                .engine(NotificationEngine.TELEGRAM)
                .receiver("[internal]")
                .parameters(Map.of("message", "hi"))
                .build();
        redisTemplate.opsForList().leftPush(CacheUtil.NOTIFICATION_PROCESSING, JsonHelper.toJson(dto));

        scheduler.job();

        ArgumentCaptor<NotificationDto> sent = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationService).sendInternal(sent.capture());
        assertEquals(dto.getId(), sent.getValue().getId());
        assertEquals(0L, redisTemplate.opsForList().size(CacheUtil.NOTIFICATION_PROCESSING));
        assertEquals(0L, redisTemplate.opsForList().size(CacheUtil.NOTIFICATION_RETRY_KEY));
    }

    @Test
    @DisplayName("job: an unreadable notification goes to DLQ once and does not stay in processing")
    void job_MalformedNotification_MovedToDlqAndRemoved() {
        redisTemplate.opsForList().leftPush(CacheUtil.NOTIFICATION_RETRY_KEY, "not json");

        scheduler.job();
        scheduler.job();

        verify(notificationRepository, times(1)).save(any());
        verifyNoInteractions(notificationService);
        assertEquals(List.of(), redisTemplate.opsForList().range(CacheUtil.NOTIFICATION_PROCESSING, 0, -1));
    }

    private void clearQueues() {
        redisTemplate.delete(List.of(CacheUtil.NOTIFICATION_PROCESSING, CacheUtil.NOTIFICATION_RETRY_KEY,
                CacheUtil.NOTIFICATION_DELAYED_KEY));
    }
}
