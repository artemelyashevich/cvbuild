package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.dto.ai.AiResponse;

public interface AiService {

    AiResponse call(AiRequestDto dto);
}
