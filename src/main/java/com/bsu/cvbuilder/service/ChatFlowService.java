package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.dto.ChatRequestDto;
import com.bsu.cvbuilder.dto.ChatResponseDto;

public interface ChatFlowService {

    ChatResponseDto message(ChatRequestDto dto);

    ChatResponseDto processAnswer(ChatRequestDto dto);
}
