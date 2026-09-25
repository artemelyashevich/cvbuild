package com.bsu.cvbuilder.service.listener;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import com.bsu.cvbuilder.domain.dto.notification.WsType;
import com.bsu.cvbuilder.domain.event.ResumeNotificationEvent;
import com.bsu.cvbuilder.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ResumeNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onResumeNotification(ResumeNotificationEvent event) {
        switch (event.kind()) {
            case GENERATED -> {
                sendWs(event.login(), Map.of("message", "Резюме сгенерировано, проверьте email", "type", WsType.SUCCESS));
                sendEmail(event, "success", "resume_success");
            }
            case GENERATION_FAILED -> {
                sendWs(event.login(), Map.of("message", "Произошла ошибка во время генерации резюме, попробуйте еще раз позже...", "type", WsType.ERROR));
                sendEmail(event, "rejected", "resume_rejected");
            }
            case ATS_ACCEPTED ->
                    sendWs(event.login(), Map.of("message", "Резюме успешно отправлено в обработку!", "status", WsType.SUCCESS));
            case ATS_FAILED -> {
                sendWs(event.login(), Map.of("message", "Ошибка адаптации резюме!", "status", WsType.ERROR));
                sendEmail(event, "rejected", "resume_rejected");
            }
            case FORM_GENERATED ->
                    sendWs(event.login(), Map.of("message", "Конструктор готов!", "type", WsType.SUCCESS));
        }
    }

    private void sendWs(String login, Map<String, Object> parameters) {
        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.WS)
                .receiver(login)
                .parameters(parameters)
                .build());
    }

    private void sendEmail(ResumeNotificationEvent event, String status, String templateName) {
        Map<String, Object> params = new HashMap<>();
        params.put("resumeId", event.resumeId());
        params.put("status", status);
        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.EMAIL)
                .receiver(event.email())
                .parameters(params)
                .templateName(templateName)
                .build());
    }
}
