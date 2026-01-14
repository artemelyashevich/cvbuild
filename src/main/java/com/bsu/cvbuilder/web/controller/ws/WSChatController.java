package com.bsu.cvbuilder.web.controller.ws;

import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.service.ChatStreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WSChatController {

    private final ChatStreamingService chatStreamingService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/ai.interview")
    public void streamChat(AiRequestDto request) {
        String destination = "/topic/chat/" + request.chatId();

        chatStreamingService.process(request)
                .subscribe(
                        token -> messagingTemplate.convertAndSend(destination, token),
                        error -> messagingTemplate.convertAndSend(destination, "Error: " + error.getMessage()),
                        () -> messagingTemplate.convertAndSend(destination, "[DONE]")
                );
    }
}