package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import com.bsu.cvbuilder.service.NotificationStrategy;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.twilio.rest.api.v2010.account.Message;

import java.util.Map;

@Slf4j
@Service("sms")
@ConditionalOnProperty(
        prefix = "app.twilio",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class SmsNotificationStrategyImpl implements NotificationStrategy {

    private final ApplicationProperties applicationProperties;

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        log.debug("Sending notification to sms");
        Message.creator(
                new PhoneNumber(notificationDto.getReceiver()),
                new PhoneNumber(applicationProperties.getTwilio().getPhoneNumber()),
                createMessage(notificationDto.getParameters())
        ).create();
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.SMS;
    }

    private String createMessage(Map<String, Object> params) {
        return params.get("message").toString();
    }
}
