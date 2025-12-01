package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.dto.ChatRequestDto;
import com.bsu.cvbuilder.service.ChatFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFlowServiceImpl implements ChatFlowService {

    @Override
    public void message(ChatRequestDto dto) {
        log.debug("--- CHAT FLOW SERVICE: STARTED - id: {} / isOptions: {} / options: {} ---",
                dto.chatId(),
                dto.isOptions(),
                dto.chatOptions()
        );


    }

    public void processMessage(ChatRequestDto dto) {
        log.debug("--- CHAT FLOW SERVICE: PROCESSING - id: {} ---", dto.chatId());


    }
}
