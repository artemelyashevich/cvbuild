package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.ResumeNotificationEvent;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.AnalyzerService;
import com.bsu.cvbuilder.service.AtsService;
import com.bsu.cvbuilder.service.JobParserService;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtsServiceImpl implements AtsService {

    private final ResumeService resumeService;
    private final JobParserService jobParserService;
    private final AiService aiService;
    private final AnalyzerService analyzerService;
    private final SecurityService securityService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Qualifier("taskFlowExecutor")
    private final Executor executor;

    @Override
    public Resume optimize(Resume resume, String jobLink) {
        return optimizeParsed(resume, jobParserService.parse(jobLink));
    }

    @Override
    public void optimizeAsync(String resumeId, String jobLink) {
        log.debug("ATS for resume: {} and vacancy: {}", resumeId, MaskUtil.mask(jobLink, 10));
        UserProfile user = securityService.findCurrentUser();
        Resume resume = resumeService.findById(resumeId);
        String jobDescription = jobParserService.parse(jobLink);
        publish(user, resumeId, ResumeNotificationEvent.Kind.ATS_ACCEPTED);

        CompletableFuture.runAsync(() -> optimizeParsed(resume, jobDescription), executor)
                .exceptionally(e -> {
                    log.error("ATS processing failed for resume {}", resumeId, e);
                    publish(user, resumeId, ResumeNotificationEvent.Kind.ATS_FAILED);
                    return null;
                });
    }

    private Resume optimizeParsed(Resume resume, String jobDescription) {
        String expandedJob = aiService.callExpansion(jobDescription).content();
        log.info("Job expanded for resume: {}", resume.getId());
        analyzerService.ats(resume, expandedJob);
        return resume;
    }

    private void publish(UserProfile user, String resumeId, ResumeNotificationEvent.Kind kind) {
        applicationEventPublisher.publishEvent(new ResumeNotificationEvent(user.getLogin(), user.getEmail(), resumeId, kind));
    }
}
