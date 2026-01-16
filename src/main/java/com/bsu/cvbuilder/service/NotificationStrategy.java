package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;

public interface NotificationStrategy {

    void sendNotification(NotificationDto notificationDto);
}
