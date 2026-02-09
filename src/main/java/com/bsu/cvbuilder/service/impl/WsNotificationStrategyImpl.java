package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.service.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service("ws")
@RequiredArgsConstructor
public class WsNotificationStrategyImpl implements NotificationStrategy {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        log.debug("Sending notification: {}", notificationDto);
        messagingTemplate.convertAndSendToUser(
                notificationDto.getReceiver(),
                "/queue/notifications",
                notificationDto.getParameters()
        );
        log.info("Sent notification: {}", notificationDto);
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.WS;
    }
}
