package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.entity.chat.AiChat;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiChatRepository extends MongoRepository<AiChat, String> {
}
