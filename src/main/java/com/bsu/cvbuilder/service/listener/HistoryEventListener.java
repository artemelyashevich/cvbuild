package com.bsu.cvbuilder.service.listener;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.domain.event.AbstractEvent;
import com.bsu.cvbuilder.service.HistoryService;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.util.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryEventListener {

    private final HistoryService historyService;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void storeEvent(AbstractEvent event) {
        historyService.save(event);
        String eventIdentifier = UUID.randomUUID().toString();
        String dateIdentifier = LocalDate.now().toString();
        String message = "[EVENT-%s-%s] \n User with id: %s.\n EVENT : %s".formatted(
                eventIdentifier,
                dateIdentifier,
                event.getUserId(),
                event.getData().get("event")
        );

        if (event.getData().get("status") != null) {
            String status = event.getData().get("status").toString();
            message += "\n STATUS: %s".formatted(status);
        }

        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.TELEGRAM)
                .parameters(Map.of("message", message))
                .build());
        log.info("[EVENT]-{}-{}", eventIdentifier, dateIdentifier);
    }
}
