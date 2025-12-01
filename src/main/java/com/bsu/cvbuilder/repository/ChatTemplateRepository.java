package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.entity.template.ChatTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatTemplateRepository extends MongoRepository<ChatTemplate, String> {
}
