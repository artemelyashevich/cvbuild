package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.template.CreateTemplateRequest;
import com.bsu.cvbuilder.domain.entity.ResumeTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResumeTemplateService {

    ResumeTemplate findByName(String name);

    ResumeTemplate create(CreateTemplateRequest request);

    ResumeTemplate save(ResumeTemplate resumeTemplate);

    Page<ResumeTemplate> findAll(Pageable pageable);
}
