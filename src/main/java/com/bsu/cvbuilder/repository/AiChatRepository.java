package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.chat.AiChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface AiChatRepository extends MongoRepository<AiChat, UUID> {
    Page<AiChat> findAllByUserId(Pageable pageable, String id);
}
