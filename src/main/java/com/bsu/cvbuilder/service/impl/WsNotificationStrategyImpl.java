package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import com.bsu.cvbuilder.service.NotificationStrategy;
import com.bsu.cvbuilder.util.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Each instance has its own in-memory STOMP broker, so a user's session lives on one instance only.
 * A notification is delivered to local sessions and published on {@link #BROADCAST_CHANNEL}; every other instance
 * delivers it to its own sessions. The broker drops it where the user is not connected.
 */
@Slf4j
@Service("ws")
public class WsNotificationStrategyImpl implements NotificationStrategy {

    public static final String BROADCAST_CHANNEL = "cvbuilder:ws:notifications";
    private static final String DESTINATION = "/queue/notifications";

    private final String instanceId = UUID.randomUUID().toString();
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    public WsNotificationStrategyImpl(SimpMessagingTemplate messagingTemplate, StringRedisTemplate redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Monitored(value = "sending_ws", context = "internal")
    public void sendNotification(NotificationDto notificationDto) {
        log.debug("Sending notification: {}", notificationDto);
        deliverLocally(notificationDto.getReceiver(), notificationDto.getParameters());
        publish(new Broadcast(instanceId, notificationDto.getReceiver(), notificationDto.getParameters()));
        log.info("Sent notification: {}", notificationDto);
    }

    /**
     * Handles a message from {@link #BROADCAST_CHANNEL}; messages sent by this instance are already delivered.
     */
    public void onBroadcast(String message) {
        Broadcast broadcast = (Broadcast) JsonHelper.fromJson(message, Broadcast.class);
        if (broadcast == null || broadcast.receiver() == null) {
            log.warn("Malformed WS broadcast: {}", message);
            return;
        }
        if (!instanceId.equals(broadcast.origin())) {
            deliverLocally(broadcast.receiver(), broadcast.parameters());
        }
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.WS;
    }

    private void deliverLocally(String receiver, Map<String, Object> parameters) {
        messagingTemplate.convertAndSendToUser(receiver, DESTINATION, parameters);
    }

    private void publish(Broadcast broadcast) {
        // Not rethrown: local sessions already got the message, and a retry would deliver it to them twice.
        // The retry queue lives in Redis too, so it cannot help while Redis is down.
        try {
            redisTemplate.convertAndSend(BROADCAST_CHANNEL, JsonHelper.toJson(broadcast));
        } catch (Exception e) {
            log.error("Failed to broadcast WS notification for {} to other instances", broadcast.receiver(), e);
        }
    }

    public record Broadcast(String origin, String receiver, Map<String, Object> parameters) {
    }
}
