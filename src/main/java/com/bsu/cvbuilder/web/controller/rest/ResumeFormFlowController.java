package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.annotation.agreement.AgreementRequire;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.flow.form.ResumeFlowService;
import com.bsu.cvbuilder.service.flow.form.domain.ResumePayload;
import com.bsu.cvbuilder.web.dto.resume.ResumeResponseDto;
import com.bsu.cvbuilder.web.mapper.impl.ResumeResponseDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/resume-flow")
@RequiredArgsConstructor
public class ResumeFormFlowController {

    private final ResumeFlowService resumeFlowService;
    private final SecurityService securityService;
    private final ResumeResponseDtoMapper resumeResponseDtoMapper;

    @AgreementRequire
    @GetMapping("/roadmap")
    public Map<String, Object> roadmap() {
        return resumeFlowService.getResumeFlowRoadmap();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponseDto generateResume(@RequestBody ResumePayload resumePayload) {
        UserProfile userProfile = securityService.findCurrentUser();
        return resumeResponseDtoMapper.toDto(resumeFlowService.generateResume(resumePayload, userProfile));
    }

    @PostMapping("/ats")
    public ResumeResponseDto ats(@RequestBody Map<String, String> resumePayload) {
        return resumeResponseDtoMapper.toDto(resumeFlowService.ats(resumePayload.get("id"), resumePayload.get("jobLink")));
    }
}
