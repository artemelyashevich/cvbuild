package com.bsu.cvbuilder.web.controller.ws;

import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.service.ChatStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import reactor.core.Disposable;
import reactor.core.Disposables;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WSChatController {

    private final ChatStreamingService chatStreamingService;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, Disposable.Swap> subscriptions = new ConcurrentHashMap<>();

    @MessageMapping("/ai.interview")
    public void streamChat(AiRequestDto request, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        if (principal == null) {
            log.warn("Rejected unauthenticated AI stream request for chat {}", request.chatId());
            return;
        }
        String sessionId = headerAccessor.getSessionId();
        String destination = "/topic/chat/" + request.chatId();

        Disposable subscription = chatStreamingService.process(request, principal.getName())
                .subscribe(
                        token -> messagingTemplate.convertAndSend(destination, token),
                        error -> log.error("WS Error", error)
                );

        subscriptions.computeIfAbsent(sessionId, id -> Disposables.swap()).update(subscription);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Disposable.Swap sub = subscriptions.remove(event.getSessionId());
        if (sub != null) {
            sub.dispose();
        }
    }
}