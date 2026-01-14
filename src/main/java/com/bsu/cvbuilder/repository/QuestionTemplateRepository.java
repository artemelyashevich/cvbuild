package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.chat.Difficulty;
import com.bsu.cvbuilder.domain.entity.chat.QuestionTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuestionTemplateRepository extends MongoRepository<QuestionTemplate, String> {
    List<QuestionTemplate> findByDifficulty(Difficulty difficulty);
}