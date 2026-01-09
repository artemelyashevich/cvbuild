package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import org.springframework.ai.chat.client.ChatClient;

import java.util.UUID;

public interface AiService {

    String call(AiRequestDto dto);

    ChatClient.CallResponseSpec callExtractor(String history, UUID chatId);

    String callExpand(String text);
}
