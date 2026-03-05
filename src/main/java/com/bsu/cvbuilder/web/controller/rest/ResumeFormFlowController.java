package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.flow.form.ResumeFlowService;
import com.bsu.cvbuilder.service.flow.form.domain.ResumePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/resume-flow")
@RequiredArgsConstructor
public class ResumeFormFlowController {

    private final ResumeFlowService resumeFlowService;
    private final SecurityService securityService;

    @GetMapping("/roadmap")
    public Map<String, Object> roadmap() {
        return resumeFlowService.getResumeFlowRoadmap();
    }

    @PostMapping
    public Resume generateResume(@RequestBody ResumePayload resumePayload) {
        UserProfile userProfile = securityService.findCurrentUser();
        return resumeFlowService.generateResume(resumePayload, userProfile);
    }
}
