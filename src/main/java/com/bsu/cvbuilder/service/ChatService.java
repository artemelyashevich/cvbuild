package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.dto.ChatRequestDto;
import com.bsu.cvbuilder.entity.chat.AiChat;

public interface ChatService {

    void create();

    AiChat findByChatId(String chatId);

    void delete(String chatId);
}
