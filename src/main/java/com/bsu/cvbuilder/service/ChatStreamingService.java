package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import reactor.core.publisher.Flux;

public interface ChatStreamingService {

    Flux<String> process(AiRequestDto aiRequestDto);
}
