package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;

public interface NotificationService {

    void sendNotification(NotificationDto notificationDto);
}
