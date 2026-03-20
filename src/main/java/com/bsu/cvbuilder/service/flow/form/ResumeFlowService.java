package com.bsu.cvbuilder.service.flow.form;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.service.flow.form.domain.ResumeField;
import com.bsu.cvbuilder.service.flow.form.domain.ResumePayload;

import java.util.Map;

public interface ResumeFlowService {

    Map<String, Object> getResumeFlowRoadmap();

    Resume generateResume(ResumePayload resumePayload, UserProfile userProfile);

    Resume regenerateField(String resumeId, ResumeField resumeField);

    Resume ats(String resumeId, String jobLink);
}
