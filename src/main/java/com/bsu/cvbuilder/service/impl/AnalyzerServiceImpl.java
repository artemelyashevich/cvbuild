package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.entity.resume.Resume;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.AnalyzerService;
import com.bsu.cvbuilder.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerServiceImpl implements AnalyzerService {

    private final ResumeService resumeService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public String analyze(String resumeId) {
        log.debug("Attempting to analyze resume {}", resumeId);
        Resume resume = resumeService.findById(resumeId);
        String response = aiService.callAnalyzer(objectMapper.writeValueAsString(resume), UUID.fromString(resume.getChatId()));
        log.info("Analyzed resume {}", resumeId);
        return response;
    }
}
