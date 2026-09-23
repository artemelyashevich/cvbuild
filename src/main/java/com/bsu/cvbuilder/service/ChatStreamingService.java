package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import reactor.core.publisher.Flux;

public interface ChatStreamingService {

    /**
     * @param login login of the authenticated STOMP user; {@code aiRequestDto.userId()} is ignored
     */
    Flux<String> process(AiRequestDto aiRequestDto, String login);
}
