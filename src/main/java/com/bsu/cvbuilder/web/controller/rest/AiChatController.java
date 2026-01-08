package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.AiTemplateMessage;
import com.bsu.cvbuilder.entity.resume.ResumeData;
import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatClient chatClient;
    private final ResumeService resumeDataExtractorService;

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

    @GetMapping("/{chatId}")
    public ResumeData extract(@PathVariable String chatId) {
        return resumeDataExtractorService.extract(UUID.fromString(chatId));
    }
}
