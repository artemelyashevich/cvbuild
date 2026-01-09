package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.entity.resume.Resume;
import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ResumeService resumeDataExtractorService;
    private final AiService aiService;

    @PostMapping
    public String ask(@RequestBody AiRequestDto aiRequestDto) {
       return aiService.call(aiRequestDto);
    }

    @GetMapping("/{chatId}")
    public Resume extract(@PathVariable String chatId) {
        return resumeDataExtractorService.findByChatId(UUID.fromString(chatId));
    }
}
