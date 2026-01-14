package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.chat.ChatSession;

public interface HistoryService {

    ChatSession findById(Long id);
}
