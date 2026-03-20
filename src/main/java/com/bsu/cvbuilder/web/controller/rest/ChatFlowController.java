package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.annotation.agreement.AgreementRequire;
import com.bsu.cvbuilder.annotation.email.EmailVerification;
import com.bsu.cvbuilder.service.flow.chat.ChatFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ChatFlowController {

    private final ChatFlowService chatFlowService;

    @AgreementRequire
    @EmailVerification
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@PathVariable UUID chatId, @RequestBody String message) {
        return chatFlowService.processMessage(chatId, message);
    }

    @AgreementRequire
    @EmailVerification
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/generate/{chatId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String generate(@PathVariable UUID chatId) {
        return chatFlowService.extractFromChat(chatId).getId();
    }

    @AgreementRequire
    @EmailVerification
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/ats/{chatId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String ats(@PathVariable String chatId, @RequestBody Map<String, String> body) {
        return chatFlowService.ats(UUID.fromString(chatId), body.get("url"));
    }
}
