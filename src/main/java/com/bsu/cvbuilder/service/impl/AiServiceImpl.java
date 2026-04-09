package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.annotation.limit.Limited;
import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.CallAnalyzerEvent;
import com.bsu.cvbuilder.domain.event.CallAtsEvent;
import com.bsu.cvbuilder.domain.event.CallExtractorEvent;
import com.bsu.cvbuilder.domain.event.UserGenerateNewMessageEvent;
import com.bsu.cvbuilder.exception.AppException;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final String PROMPT_INTERVIEWER = "interviewer";
    private static final String PROMPT_FINAL = "final";
    private static final String PROMPT_EXTRACTOR = "extractor";
    private static final String PROMPT_ANALYZER = "analyzer";
    private static final String PROMPT_ATS = "ats";
    private static final String PROMPT_EXPANSION = "resume_expansion";
    private static final String COMPLETED_SIGNAL = "COMPLETED";

    private final ChatClient chatClient;
    private final PromptRegistryService promptRegistryService;
    private final ApplicationProperties applicationProperties;
    private final SecurityService securityService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Executor executor;

    @Override
    @Limited(value = LimitType.AI_MESSAGE, capacity = 20)
    @Monitored(value = "calling_ai_interviewer", context = "ai")
    public String callFlow(AiRequestDto dto) {
        log.debug("AI Call [INTERVIEWER] for chatId: {}", dto.chatId());

        String systemPrompt = promptRegistryService.getPrompt(PROMPT_INTERVIEWER);

        String response = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, dto.chatId()))
                .system(systemPrompt)
                .user(dto.content())
                .call()
                .content();
        log.info("[RESPONSE]: {}", response);
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
    @Transactional
    @Monitored(value = "calling_ai_extractor", context = "ai")
    public ChatClient.CallResponseSpec callExtractor(String history, UUID chatId) {
        UserProfile userProfile = securityService.findCurrentUser();
        log.debug("AI Call [EXTRACTOR] for chatId: {}", chatId);
        String extractorPrompt = promptRegistryService.getPrompt(PROMPT_EXTRACTOR);
        eventPublisher.publishEvent(new CallExtractorEvent(userProfile.getId()));
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "ignore"))
                .user(u -> u.text(extractorPrompt.formatted(history)))
                .options(defaultOptions())
                .call();
    }

    @Override
    public ChatClient.CallResponseSpec callExpansion(Resume resume) {
        log.debug("AI Call [EPANSION] for resume: {}", resume.getId());
        String expansionPrompt = promptRegistryService.getPrompt(PROMPT_EXPANSION);
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "ignore"))
                .user(u -> u.text(expansionPrompt.formatted(JsonHelper.toJson(resume.getBlocks()))))
                .options(defaultOptions())
                .call();
    }

    @Override
    @Transactional
    @Monitored(value = "calling_ai_analyzer", context = "ai")
    public String callAnalyzer(String text, UUID chatId) {
        UserProfile userProfile = securityService.findCurrentUser();
        log.debug("AI Call [ANALYZER] for chatId: {}", chatId);

        String analyzerPrompt = promptRegistryService.getPrompt(PROMPT_ANALYZER);
        PromptTemplate template = new PromptTemplate(analyzerPrompt);
        String renderedPrompt = template.render(Map.of(
                "generated_resume", text,
                "job_description", ""
        ));

        eventPublisher.publishEvent(new CallAnalyzerEvent(userProfile.getId()));

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(renderedPrompt)
                .options(defaultOptions())
                .call()
                .content();
    }

    @Override
    @Transactional
    @Monitored(value = "calling_ai_ats_optimization", context = "ai")
    public ChatClient.CallResponseSpec callAtsOptimization(Resume resume, String jobDescription) {
        UserProfile userProfile = securityService.findCurrentUser();
        log.debug("AI Call [ATS_OPTIMIZATION] for resume with id: {}]", resume.getId());

        String atsPrompt = promptRegistryService.getPrompt(PROMPT_ATS);
        String renderedPrompt = null;
        try {
            renderedPrompt = atsPrompt.formatted(objectMapper.writeValueAsString(resume.getBlocks()), jobDescription);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            throw new AppException(e, 500);
        }

        eventPublisher.publishEvent(new CallAtsEvent(userProfile.getId()));

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "ignore"))
                .user(renderedPrompt)
                .options(OllamaOptions.builder()
                        .temperature((double) 0)
                        .numPredict(2000)
                        .build())
                .call();
    }

    @Override
    public CompletableFuture<Object> callFlow(String promptName, String content) {
        return CompletableFuture.supplyAsync(
                () -> {
                    String prompt = promptRegistryService.getFlowPrompt(promptName);
                    log.debug("AI CALL [FLOW] prompt name: {}", promptName);
                    if (content != null) {
                        prompt = prompt.formatted(content);
                    }
                    log.debug("Current thread: {}", Thread.currentThread().getName());
                    return chatClient.prompt()
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "ignore"))
                            .user(prompt)
                            .options(OllamaOptions.builder()
                                    .temperature((double) 0)
                                    .numPredict(2000)
                                    .build())
                            .call()
                            .content();
                }, executor
        );
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