package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.chat.AiChat;

import java.util.UUID;

public interface ChatService {

    AiChat createAiChat(UUID chatId);

    AiChat getChatById(UUID chatId);

    AiChat saveAiChat(AiChat aiChat);
}
