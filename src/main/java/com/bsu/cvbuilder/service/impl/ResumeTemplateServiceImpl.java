package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.template.CreateTemplateRequest;
import com.bsu.cvbuilder.domain.entity.ResumeTemplate;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.ResumeTemplateRepository;
import com.bsu.cvbuilder.service.ResumeTemplateService;
import com.bsu.cvbuilder.service.mapper.ResumeTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeTemplateServiceImpl implements ResumeTemplateService {

    private final ResumeTemplateMapper resumeTemplateMapper;
    private final MongoTemplate mongoTemplate;
    private final ResumeTemplateRepository resumeTemplateRepository;

    @Override
    public ResumeTemplate findById(String name) {
        log.debug("Attempting to find resume template by id {}", name);
        ResumeTemplate resumeTemplate = resumeTemplateRepository.findById(name).orElseThrow(
                () -> {
                    String message = String.format("Resume template with id %s not found", name);
                    log.info(message);
                    return new AppException(message, 404);
                }
        );
        log.info("Resume template with id {} found", name);
        return resumeTemplate;
    }

    @Override
    @Transactional
    public ResumeTemplate create(CreateTemplateRequest request) {
        log.debug("Attempting to create resume template {}", request);
        if (resumeTemplateRepository.existsByName(request.name())) {
            String message = String.format("Resume template with name %s already exists", request.name());
            log.info(message);
            throw new AppException(message, 400);
        }
        ResumeTemplate resumeTemplate = resumeTemplateMapper.toEntity(request);
        ResumeTemplate resumeTemplateSaved = resumeTemplateRepository.save(resumeTemplate);
        log.info("Resume template saved {}", resumeTemplateSaved);
        return resumeTemplateSaved;
    }

    @Override
    public ResumeTemplate save(ResumeTemplate resumeTemplate) {
        log.debug("Attempting to save resume template {}", resumeTemplate);
        ResumeTemplate savedResumeTemplate = resumeTemplateRepository.save(resumeTemplate);
        log.info("Resume template with name {} saved", resumeTemplate.getName());
        return savedResumeTemplate;
    }

    @Override
    public Page<ResumeTemplate> findAll(Pageable pageable) {
        log.debug("Attempting to find all resume templates: {}", pageable);

        Query query = new Query().with(pageable);

        List<ResumeTemplate> list = mongoTemplate.find(query, ResumeTemplate.class);

        return PageableExecutionUtils.getPage(
                list,
                pageable,
                () -> mongoTemplate.count(
                        Query.of(query),
                        ResumeTemplate.class
                )
        );
    }
}
