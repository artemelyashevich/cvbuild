package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.service.flow.ChatFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ChatFlowController {

    private final ChatFlowService chatFlowService;

    @PostMapping(value = "/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@PathVariable UUID chatId, @RequestBody String message) {
        return chatFlowService.processMessage(chatId, message);
    }
}
