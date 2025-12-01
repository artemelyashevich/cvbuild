package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.template.ChatTemplate;
import com.bsu.cvbuilder.repository.ChatTemplateRepository;
import com.bsu.cvbuilder.service.ChatTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTemplateServiceImpl implements ChatTemplateService {

    private final ChatTemplateRepository chatTemplateRepository;

    @Override
    public void create() {

    }

    @Override
    public ChatTemplate findChatTemplate(String chatTemplateId) {
        return null;
    }
}
