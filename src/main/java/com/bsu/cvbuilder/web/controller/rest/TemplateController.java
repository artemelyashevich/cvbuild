package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.dto.template.CreateTemplateRequest;
import com.bsu.cvbuilder.domain.entity.ResumeTemplate;
import com.bsu.cvbuilder.service.ResumeTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeTemplate save(@RequestBody CreateTemplateRequest resumeTemplate) {
        return resumeTemplateService.create(resumeTemplate);
    }

    @PatchMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeTemplate update(@RequestBody ResumeTemplate resumeTemplate) {
        return resumeTemplateService.save(resumeTemplate);
    }
}
