package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.entity.chat.ChatSession;
import com.bsu.cvbuilder.entity.chat.SessionState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    Optional<ChatSession> findByUserIdAndState(String userId, SessionState state);

    List<ChatSession> findByUserIdOrderByCreatedAtDesc(String userId);
}