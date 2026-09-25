package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.cache.SecureDataCacheSingleton;
import com.bsu.cvbuilder.configuration.RedisPubSubConfiguration;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import com.bsu.cvbuilder.domain.dto.notification.WsType;
import com.bsu.cvbuilder.service.impl.WsNotificationStrategyImpl;
import com.bsu.cvbuilder.support.RedisTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Two strategies with their own listener containers on one real Redis act as two application instances.
 */
class WsNotificationBroadcastTest {

    private LettuceConnectionFactory connectionFactory;
    private RedisMessageListenerContainer containerA;
    private RedisMessageListenerContainer containerB;
    private SimpMessagingTemplate brokerA;
    private SimpMessagingTemplate brokerB;
    private WsNotificationStrategyImpl instanceA;

    @BeforeEach
    void setUp() throws Exception {
        connectionFactory = RedisTestSupport.newConnectionFactory();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        brokerA = mock(SimpMessagingTemplate.class);
        brokerB = mock(SimpMessagingTemplate.class);
        instanceA = new WsNotificationStrategyImpl(brokerA, redisTemplate);
        WsNotificationStrategyImpl instanceB = new WsNotificationStrategyImpl(brokerB, redisTemplate);
        RedisPubSubConfiguration configuration = new RedisPubSubConfiguration();
        containerA = start(configuration.redisMessageListenerContainer(connectionFactory, mock(SecureDataCacheSingleton.class), instanceA));
        containerB = start(configuration.redisMessageListenerContainer(connectionFactory, mock(SecureDataCacheSingleton.class), instanceB));
    }

    @AfterEach
    void tearDown() throws Exception {
        containerA.destroy();
        containerB.destroy();
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("a notification sent on one instance reaches the user's session on another instance, once per instance")
    void sendNotification_DeliveredOnEveryInstanceOnce() throws InterruptedException {
        instanceA.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.WS)
                .receiver("alice")
                .parameters(Map.of("message", "Резюме готово", "type", WsType.SUCCESS))
                .build());

        verify(brokerB, timeout(5_000)).convertAndSendToUser("alice", "/queue/notifications",
                Map.of("message", "Резюме готово", "type", "SUCCESS"));
        Thread.sleep(300);
        verify(brokerA, times(1)).convertAndSendToUser(eq("alice"), eq("/queue/notifications"), any(Object.class));
    }

    @Test
    @DisplayName("Redis being down does not fail the notification or block local delivery")
    void sendNotification_RedisDown_StillDeliveredLocally() {
        StringRedisTemplate broken = mock(StringRedisTemplate.class);
        doThrow(new IllegalStateException("redis down")).when(broken).convertAndSend(anyString(), anyString());
        SimpMessagingTemplate broker = mock(SimpMessagingTemplate.class);
        WsNotificationStrategyImpl strategy = new WsNotificationStrategyImpl(broker, broken);

        assertDoesNotThrow(() -> strategy.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.WS)
                .receiver("alice")
                .parameters(Map.of("message", "hi"))
                .build()));

        verify(broker).convertAndSendToUser("alice", "/queue/notifications", Map.of("message", "hi"));
    }

    @Test
    @DisplayName("a malformed broadcast is ignored")
    void onBroadcast_Malformed_Ignored() {
        instanceA.onBroadcast("not json");

        verifyNoInteractions(brokerA);
    }

    private static RedisMessageListenerContainer start(RedisMessageListenerContainer container) throws Exception {
        container.afterPropertiesSet();
        container.start();
        return container;
    }
}
