package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.event.LogoutEvent;
import com.bsu.cvbuilder.service.HistoryService;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.listener.HistoryEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HistoryEventListenerTest {

    @Mock
    private HistoryService historyService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private HistoryEventListener listener;

    private String sentMessage() {
        ArgumentCaptor<NotificationDto> dto = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationService).sendNotification(dto.capture());
        return dto.getValue().getParameters().get("message").toString();
    }

    @Test
    @DisplayName("storeEvent: status from event data is included in the notification")
    void storeEvent_WithStatus_AddsStatusLine() {
        var event = LogoutEvent.builder().userId("user-1").build();
        event.setData(Map.of("login", "alice", "status", "success"));

        listener.storeEvent(event);

        verify(historyService).save(event);
        assertTrue(sentMessage().contains("STATUS: success"));
    }

    @Test
    @DisplayName("storeEvent: event without data has no status line")
    void storeEvent_WithoutData_NoStatusLine() {
        listener.storeEvent(LogoutEvent.builder().userId("user-1").build());

        assertFalse(sentMessage().contains("STATUS"));
    }
}
