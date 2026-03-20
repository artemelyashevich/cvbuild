package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.dto.template.CreateTemplateRequest;
import com.bsu.cvbuilder.domain.entity.ResumeTemplate;
import com.bsu.cvbuilder.service.ResumeTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final ResumeTemplateService resumeTemplateService;

    @GetMapping
    public Page<ResumeTemplate> findAll(@RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
                                        @RequestParam(name = "size", defaultValue = "5", required = false) Integer size) {
        return resumeTemplateService.findAll(Pageable
                .ofSize(size)
                .withPage(page));
    }

    @GetMapping("/{id}")
    public ResumeTemplate findById(@PathVariable String id) {
        return resumeTemplateService.findById(id);
    }

    @PostMapping
    public ResumeTemplate save(@RequestBody CreateTemplateRequest resumeTemplate) {
        return resumeTemplateService.create(resumeTemplate);
    }

    @PatchMapping
    public ResumeTemplate update(@RequestBody ResumeTemplate resumeTemplate) {
        return resumeTemplateService.save(resumeTemplate);
    }
}
