package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.web.dto.resume.CreateResumeRequest;
import com.bsu.cvbuilder.web.dto.resume.ResumeResponse;
import com.bsu.cvbuilder.web.mapper.impl.ResumeDtoMapper;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private static final ResumeDtoMapper mapper = Mappers.getMapper(ResumeDtoMapper.class);

    private final ResumeService resumeService;

    @GetMapping
    public List<ResumeResponse> findAll() {
        return mapper.toResponseList(null);
    }

    @GetMapping("/{resumeId}")
    public ResumeResponse findOne(@PathVariable("resumeId") String resumeId) {
        return mapper.toResponse(null);
    }

    @PostMapping
    public ResumeResponse create(@Validated @RequestBody CreateResumeRequest createResumeRequest) {
        return mapper.toResponse(null);
    }
}
