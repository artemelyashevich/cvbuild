package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.chat.ChatSession;

public interface HistoryService {

    ChatSession findById(Long id);
}
