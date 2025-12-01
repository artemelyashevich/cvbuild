package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.dto.ChatResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @GetMapping
    public ChatResponseDto getChat() {
        return ChatResponseDto.builder().build();
    }

    @PostMapping
    public void process(){

    }
}
