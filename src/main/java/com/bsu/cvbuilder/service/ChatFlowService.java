package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.dto.chat.AnswerRequest;
import com.bsu.cvbuilder.entity.chat.ChatSession;

public interface ChatFlowService {

    ChatSession startSession(String templateId);

    ChatSession processAnswer(String sessionId, AnswerRequest answer);
}
