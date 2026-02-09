package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;

public interface NotificationStrategy {

    void sendNotification(NotificationDto notificationDto);

    NotificationEngine getSupportedEngine();
}
