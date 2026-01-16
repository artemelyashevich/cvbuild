package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.entity.resume.Resume;
import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ResumeService resumeDataExtractorService;
    private final AiService aiService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String ask(@RequestBody AiRequestDto aiRequestDto) {
       return aiService.call(aiRequestDto);
    }

    @GetMapping("/{chatId}")
    @ResponseStatus(HttpStatus.OK)
    public Resume extract(@PathVariable String chatId) {
        return resumeDataExtractorService.findByChatId(UUID.fromString(chatId));
    }
}
