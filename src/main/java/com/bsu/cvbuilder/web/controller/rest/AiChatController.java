package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatClient chatClient;

    @PostMapping
    public String ask(@RequestBody AiRequestDto aiRequestDto) {
        return chatClient.prompt(aiRequestDto.content()).call().content();
    }
}
