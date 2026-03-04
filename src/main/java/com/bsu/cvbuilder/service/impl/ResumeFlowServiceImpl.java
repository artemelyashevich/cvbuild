package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.ai.ResumeFlowFormDto;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ResumeFlowService;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeFlowServiceImpl implements ResumeFlowService {

    private final AiService aiService;
    private final ResumeService resumeService;

    @Override
    public Resume generate(ResumeFlowFormDto resumeFlowFormDto) {
        return null;
    }
}
