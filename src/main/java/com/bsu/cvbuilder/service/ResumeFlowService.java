package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.chat.ChatSession;

public interface ResumeFlowService {

    void process(ChatSession chatSession);
}
