package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.chat.Difficulty;
import com.bsu.cvbuilder.entity.chat.QuestionTemplate;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.QuestionTemplateRepository;
import com.bsu.cvbuilder.service.QuestionTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionTemplateServiceImpl implements QuestionTemplateService {

    private final QuestionTemplateRepository questionTemplateRepository;

    @Override
    @CachePut(value = "question:template::", key = "#id")
    public QuestionTemplate findById(String id) {
        log.debug("Attempting to find chat template by id {}", id);
        return questionTemplateRepository.findById(id).orElseThrow(() -> {
            var message = "Question template with id %s not found".formatted(id);
            log.debug(message);
            return new AppException(message, 404);
        });
    }

    @Override
    public Page<QuestionTemplate> findAll(Pageable pageable) {
        log.debug("Attempting to find all question templates");
        return questionTemplateRepository.findAll(pageable);
    }

    @Override
    @CachePut(value = "question:template::", key = "#result.id")
    public QuestionTemplate create(QuestionTemplate questionTemplate) {
        log.debug("Attempting to save chat template {}", questionTemplate);
        return questionTemplateRepository.save(questionTemplate);
    }

    @Override
    public List<QuestionTemplate> findByDifficulty(Difficulty difficulty) {
        log.debug("Attempting to find chat template by difficulty {}", difficulty);
        return questionTemplateRepository.findByDifficulty(difficulty);
    }

    @Override
    @CacheEvict(value = "question:template::", key = "#result.id")
    public void delete(String id) {
        log.debug("Attempting to delete chat template {}", id);
        questionTemplateRepository.deleteById(id);
    }
}
