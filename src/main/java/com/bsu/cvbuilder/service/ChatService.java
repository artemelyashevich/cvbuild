package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.chat.AiChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatService {

    AiChat createAiChat(UUID chatId);

    AiChat getChatById(UUID chatId);

    AiChat saveAiChat(AiChat aiChat);

    Page<AiChat> findAllByCurrentUser(Pageable pageable);
}
