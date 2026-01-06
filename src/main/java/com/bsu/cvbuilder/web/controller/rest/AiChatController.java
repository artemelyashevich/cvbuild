package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.ai.AiTemplateMessage;
import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatClient chatClient;

    @PostMapping
    public String ask(@RequestBody AiRequestDto aiRequestDto) {
        return chatClient.prompt()
                .advisors(
                        advisorSpec -> advisorSpec.param(
                                ChatMemory.CONVERSATION_ID,
                                aiRequestDto.chatId()
                        )
                )
                .system(s -> s.text(AiTemplateMessage.SYSTEM_INTERVIEWER.getMessage())
                        .params(Map.of(
                                "points", 100,
                                "gen_cost", 10,
                                "regen_cost", 5,
                                "context", "No data collected yet"
                        )))
                .user(
                        aiRequestDto.content()
                )
                .call()
                .content();
    }
}
