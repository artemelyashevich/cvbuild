package com.bsu.cvbuilder.controller.unit;

import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.service.ChatStreamingService;
import com.bsu.cvbuilder.web.controller.ws.WSChatController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WSChatControllerTest {

    private final ChatStreamingService chatStreamingService = mock(ChatStreamingService.class);
    private final WSChatController controller =
            new WSChatController(chatStreamingService, mock(SimpMessagingTemplate.class));

    private static SimpMessageHeaderAccessor session(String id) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(id);
        return accessor;
    }

    private static Flux<String> endlessStream(AtomicBoolean cancelled) {
        return Flux.<String>never().doOnCancel(() -> cancelled.set(true));
    }

    private static Principal principal() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("alice");
        return principal;
    }

    @Test
    @DisplayName("streamChat: a new message cancels the stream still running in the same session")
    void streamChat_SecondMessage_CancelsFirstStream() {
        AtomicBoolean firstCancelled = new AtomicBoolean();
        AtomicBoolean secondCancelled = new AtomicBoolean();
        when(chatStreamingService.process(any(), eq("alice")))
                .thenReturn(endlessStream(firstCancelled), endlessStream(secondCancelled));
        var request = new AiRequestDto(UUID.randomUUID(), null, "hi");

        controller.streamChat(request, session("s1"), principal());
        controller.streamChat(request, session("s1"), principal());

        assertTrue(firstCancelled.get());
        assertFalse(secondCancelled.get());
    }

    @Test
    @DisplayName("handleDisconnect: cancels the running stream of the session")
    void handleDisconnect_RunningStream_IsCancelled() {
        AtomicBoolean cancelled = new AtomicBoolean();
        when(chatStreamingService.process(any(), eq("alice"))).thenReturn(endlessStream(cancelled));
        controller.streamChat(new AiRequestDto(UUID.randomUUID(), null, "hi"), session("s1"), principal());

        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn("s1");
        controller.handleDisconnect(event);

        assertTrue(cancelled.get());
    }

    @Test
    @DisplayName("streamChat: unauthenticated request never reaches the model")
    void streamChat_NoPrincipal_IsIgnored() {
        controller.streamChat(new AiRequestDto(UUID.randomUUID(), null, "hi"), session("s1"), null);

        verifyNoInteractions(chatStreamingService);
    }
}
