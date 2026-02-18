package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.AnalyzerService;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerServiceImpl implements AnalyzerService {
    private static final BeanOutputConverter<Resume> converter = new BeanOutputConverter<>(Resume.class);

    private final ResumeService resumeService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final NotificationService notificationService;

    @SneakyThrows
    @Override
    public String analyze(String resumeId) {
        log.debug("Attempting to analyze resume {}", resumeId);
        Resume resume = resumeService.findById(resumeId);
        String response = aiService.callAnalyzer(objectMapper.writeValueAsString(resume), UUID.fromString(resume.getChatId()));
        log.info("Analyzed resume {}", resumeId);
        return response;
    }

    @Async
    @Override
    public void ats(Resume resume, String jobDescription, UserProfile currentUser) {
        log.debug("[ANALYZER] Attempting to analyze resume {}", resume.getChatId());
        try {
            ChatClient.CallResponseSpec callResponseSpec = aiService.callAtsOptimization(resume, jobDescription);
            Resume optimizedResume = callResponseSpec.entity(converter);
            optimizedResume.setId(null);
            optimizedResume.setChatId(resume.getChatId().toString());
            optimizedResume.setAts(true);
            transactionTemplate.execute(status -> {
                Resume persistentResume = resumeService.save(optimizedResume);
                resume.setAtsId(persistentResume.getId());
                resumeService.save(resume);
                return persistentResume;
            });
            log.info("[ANALYZER] Persisted resume {}", optimizedResume.getChatId());
            notificationService.sendNotification(NotificationDto.builder()
                            .templateName("common")
                            .engine(NotificationEngine.EMAIL)
                            .receiver(currentUser.getEmail())
                            .parameters(Map.of("message", "Congratulations! Ats - success"))
                    .build());
        }  catch (Exception e) {
            log.error("Failed to generate ats optimization for resume {}: {}", resume.getId(), e.getMessage());
            throw new AppException("Failed to generate resume via AI. Please try to chat more.", e, 500);
        }

    }
}
