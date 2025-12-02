package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.entity.chat.ChatSession;
import com.bsu.cvbuilder.service.ChatFlowService;
import com.bsu.cvbuilder.web.dto.chat.ChatResponseDto;
import com.bsu.cvbuilder.web.mapper.Mappable;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final Mappable<ChatSession, ChatResponseDto> mapper;
    private final ChatFlowService chatFlowService;

    @GetMapping
    public ChatResponseDto getChat() {
        return ChatResponseDto.builder().build();
    }

    @PostMapping
    public void process(){

    }
}
