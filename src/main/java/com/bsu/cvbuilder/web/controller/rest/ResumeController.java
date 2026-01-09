package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.entity.resume.Resume;
import com.bsu.cvbuilder.service.ResumeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Resume")
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping("/{id}")
    public Resume findById(@PathVariable String id) {
        return resumeService.findById(id);
    }
}
