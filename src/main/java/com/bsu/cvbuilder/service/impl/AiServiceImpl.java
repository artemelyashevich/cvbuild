package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.annotation.limit.Limited;
import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.UserGenerateNewMessageEvent;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.util.JsonHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final String PROMPT_INTERVIEWER = "interviewer";
    private static final String PROMPT_FINAL = "final";
    private static final String PROMPT_EXTRACTOR = "extractor";
    private static final String PROMPT_ANALYZER = "analyzer";
    private static final String PROMPT_ATS = "ats";
    private static final String COMPLETED_SIGNAL = "COMPLETED";

    private final ChatClient chatClient;
    private final PromptRegistryService promptRegistryService;
    private final ApplicationProperties applicationProperties;
    private final SecurityService securityService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Limited(value = LimitType.AI_MESSAGE, capacity = 20)
    @Monitored(value = "calling_ai_interviewer", context = "ai")
    public String call(AiRequestDto dto) {
        log.debug("AI Call [INTERVIEWER] for chatId: {}", dto.chatId());

        String systemPrompt = promptRegistryService.getPrompt(PROMPT_INTERVIEWER);

        String response = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, dto.chatId()))
                .system(systemPrompt)
                .user(dto.content())
                .call()
                .content();

        if (response == null) {
            log.warn("AI returned empty response for chatId: {}", dto.chatId());
            return "Извините, я не смог обработать ваш запрос. Попробуйте еще раз.";
        }

        publishUsageEvent();

        if (response.contains(COMPLETED_SIGNAL)) {
            return handleFinalStep(dto.chatId());
        }

        return response;
    }

    private String handleFinalStep(UUID chatId) {
        log.info("Interviewer phase completed, generating final summary for chatId: {}", chatId);
        String finalPromptText = promptRegistryService.getPrompt(PROMPT_FINAL);

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(finalPromptText)
                .call()
                .content();
    }

    @Override
    @Monitored(value = "calling_ai_extractor", context = "ai")
    public ChatClient.CallResponseSpec callExtractor(String history, UUID chatId) {
        log.debug("AI Call [EXTRACTOR] for chatId: {}", chatId);
        String extractorPrompt = promptRegistryService.getPrompt(PROMPT_EXTRACTOR);

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(u -> u.text(extractorPrompt.formatted(history)))
                .options(defaultOptions())
                .call();
    }

    @Override
    @Monitored(value = "calling_ai_analyzer", context = "ai")
    public String callAnalyzer(String text, UUID chatId) {
        log.debug("AI Call [ANALYZER] for chatId: {}", chatId);

        String analyzerPrompt = promptRegistryService.getPrompt(PROMPT_ANALYZER);
        PromptTemplate template = new PromptTemplate(analyzerPrompt);
        String renderedPrompt = template.render(Map.of(
                "generated_resume", text,
                "job_description", ""
        ));

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(renderedPrompt)
                .options(defaultOptions())
                .call()
                .content();
    }

    @Override
    @Monitored(value = "calling_ai_ats_optimization", context = "ai")
    public ChatClient.CallResponseSpec callAtsOptimization(Resume resume, String jobDescription) {
        log.debug("AI Call [ATS_OPTIMIZATION] for resume with id: {}]", resume.getId());

        String atsPrompt = promptRegistryService.getPrompt(PROMPT_ATS);
        String renderedPrompt = null;
        try {
            renderedPrompt = atsPrompt.formatted(objectMapper.writeValueAsString(resume), jobDescription);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, resume.getChatId()))
                .user(renderedPrompt)
                .options(OllamaOptions.builder()
                        .temperature((double)0)
                        .numPredict(2000)
                        .build())
                .call();
    }

    private OllamaOptions defaultOptions() {
        return OllamaOptions.builder()
                .temperature(applicationProperties.getChat().getExtractionTemperature())
                .numPredict(2000)
                .build();
    }

    private void publishUsageEvent() {
        UserProfile user = securityService.findCurrentUser();
        eventPublisher.publishEvent(new UserGenerateNewMessageEvent(user.getId()));
    }
}