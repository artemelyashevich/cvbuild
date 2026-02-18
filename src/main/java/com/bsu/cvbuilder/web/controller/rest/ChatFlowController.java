package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.annotation.agreement.AgreementRequire;
import com.bsu.cvbuilder.annotation.email.EmailVerification;
import com.bsu.cvbuilder.service.flow.ChatFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    @PostMapping(value = "/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@PathVariable UUID chatId, @RequestBody String message) {
        return chatFlowService.processMessage(chatId, message);
    }

    @AgreementRequire
    @EmailVerification
    @PostMapping(value = "/generate/{chatId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String generate(@PathVariable UUID chatId) {
        return chatFlowService.extractFromChat(chatId).getId();
    }

    @AgreementRequire
    @EmailVerification
    @PostMapping(value = "/ats/{chatId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String ats(@PathVariable String chatId, @RequestBody Map<String, String> body) {
        return chatFlowService.ats(UUID.fromString(chatId), body.get("url"));
    }
}
