package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.dto.ai.AiResponse;
import com.bsu.cvbuilder.entity.chat.AiChat;

import java.util.UUID;

public interface AiService {

    AiResponse call(AiRequestDto dto);
}
