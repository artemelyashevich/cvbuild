package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.service.NotificationStrategy;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Service("ws")
public class WsNotificationStrategyImpl implements NotificationStrategy {
    @Override
    public void sendNotification(NotificationDto notificationDto) {

    }
}
