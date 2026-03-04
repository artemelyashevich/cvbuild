package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.ai.ResumeFlowFormDto;
import com.bsu.cvbuilder.domain.entity.Resume;

public interface ResumeFlowService {

    Resume generate(ResumeFlowFormDto resumeFlowFormDto);
}
