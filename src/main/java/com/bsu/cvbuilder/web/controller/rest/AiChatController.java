package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.annotation.agreement.AgreementRequire;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ResumeService resumeDataExtractorService;
    private final AiService aiService;
    private final ChatService chatService;

    @GetMapping
    public Page<AiChat> getAiChats(
            @RequestParam(required = false, name = "page", defaultValue = "0") Integer page,
            @RequestParam(required = false, name = "size", defaultValue = "5") Integer size,
            @RequestParam(required = false, name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(required = false, name = "direction", defaultValue = "asc") String direction

    ) {
        Sort sorting = Sort.by(Sort.Direction.fromString(direction), sort);
        return chatService.findAllByCurrentUser(PageRequest.of(page, size, sorting));
    }

    @GetMapping("/chat/{chatId}")
    public AiChat findAll(@PathVariable UUID chatId) {
        return chatService.getChatById(chatId);
    }

    @AgreementRequire
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public AiChat create() {
        return chatService.createAiChat(UUID.randomUUID());
    }

    @AgreementRequire
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String ask(@RequestBody AiRequestDto aiRequestDto) {
        return aiService.call(aiRequestDto);
    }

    @AgreementRequire
    @GetMapping("/{chatId}")
    @ResponseStatus(HttpStatus.OK)
    public Resume extract(@PathVariable String chatId) {
        return resumeDataExtractorService.findByChatId(UUID.fromString(chatId));
    }
}
