package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.template.ChatTemplate;

import java.util.UUID;

public interface ChatTemplateService {

    void create();

    ChatTemplate findChatTemplate(String chatTemplateId);
}
