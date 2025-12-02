package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.chat.Difficulty;
import com.bsu.cvbuilder.entity.chat.QuestionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QuestionTemplateService {

    Page<QuestionTemplate> findAll(Pageable pageable);

    QuestionTemplate create(QuestionTemplate questionTemplate);

    List<QuestionTemplate> findByDifficulty(Difficulty difficulty);

    void delete(String id);

    QuestionTemplate findById(String id);
}
