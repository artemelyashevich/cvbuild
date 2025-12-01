package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.dto.ChatRequestDto;

public interface ChatFlowService {

    void message(ChatRequestDto dto);
}
