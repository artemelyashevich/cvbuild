package com.bsu.cvbuilder.service.flow.chat;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import com.bsu.cvbuilder.domain.dto.notification.WsType;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.ChatMessage;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.UserGenerateNewMessageEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatFlowService {

    private static final int HISTORY_LIMIT = 15;
    private static final long SSE_TIMEOUT = Duration.ofMinutes(5).toMillis();

    private final ChatClient chatClient;
    private final ChatService chatService;
    private final ResumeService resumeService;
    private final JobParserService jobParserService;
    private final AnalyzerService analyzerService;
    private final NotificationService notificationService;
    private final SecurityService securityService;
    private final Map<ChatFlowStep, AbstractChatStepHandler> stepHandlers;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ChatFlowService(ChatClient chatClient,
                           ChatService chatService,
                           ResumeService resumeService, JobParserService jobParserService, AnalyzerService analyzerService, NotificationService notificationService, SecurityService securityService,
                           List<AbstractChatStepHandler> handlers, ApplicationEventPublisher applicationEventPublisher) {
        this.chatClient = chatClient;
        this.chatService = chatService;
        this.resumeService = resumeService;
        this.jobParserService = jobParserService;
        this.analyzerService = analyzerService;
        this.notificationService = notificationService;
        this.securityService = securityService;
        this.stepHandlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AbstractChatStepHandler::getStep,
                        Function.identity()
                ));
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public Resume extractFromChat(UUID chatId) {
        log.debug("Extracting resume from chat {}", chatId);
        return resumeService.findByChatId(chatId);
    }

    public String processMessageSync(UUID chatId, String userMessage) {
        UserProfile userProfile = securityService.findCurrentUser();
        log.info("Processing sync message for chatId={}", chatId);
        applicationEventPublisher.publishEvent(new UserGenerateNewMessageEvent(userProfile.getId()));
        AiChat chat = chatService.getChatById(chatId);
        AbstractChatStepHandler currentHandler = resolveHandler(chat.getChatFlowStep());

        StepAnalysisResult analysis = analyzeStep(chat, userMessage, currentHandler);

        String systemPrompt = resolveSystemPrompt(chat, currentHandler, analysis);
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

    @Transactional
    public SseEmitter processMessage(UUID chatId, String userMessage) {
        UserProfile userProfile = securityService.findCurrentUser();
        log.info("Processing message for chatId={}", chatId);
        applicationEventPublisher.publishEvent(new UserGenerateNewMessageEvent(userProfile.getId()));
        AiChat chat = chatService.getChatById(chatId);
        AbstractChatStepHandler currentHandler = resolveHandler(chat.getChatFlowStep());

        StepAnalysisResult analysis = analyzeStep(chat, userMessage, currentHandler);

        String systemPrompt = resolveSystemPrompt(chat, currentHandler, analysis);

        return streamResponse(chatId, userMessage, systemPrompt);
    }

    private StepAnalysisResult analyzeStep(AiChat chat,
                                           String userMessage,
                                           AbstractChatStepHandler handler) {

        String validationHistory = buildHistoryForValidator(chat, userMessage);
        return handler.analyzeCompletion(chatClient, validationHistory);
    }

    private String resolveSystemPrompt(AiChat chat,
                                       AbstractChatStepHandler currentHandler,
                                       StepAnalysisResult analysis) {

        if (analysis.completed()) {
            return advanceStep(chat, currentHandler);
        }

        return buildIncompletePrompt(currentHandler, analysis);
    }

    private String advanceStep(AiChat chat,
                               AbstractChatStepHandler currentHandler) {

        ChatFlowStep nextStep = currentHandler.getNextStep();
        log.info("Step {} completed. Moving to {}", currentHandler.getStep(), nextStep);

        if (nextStep == ChatFlowStep.COMPLETED) {
            chat.setFinished(true);
        }

        chat.setChatFlowStep(nextStep);
        chatService.saveAiChat(chat);

        return resolveHandler(nextStep).getSystemPrompt();
    }

    private String buildIncompletePrompt(AbstractChatStepHandler handler,
                                         StepAnalysisResult analysis) {

        log.info("[STEP] {} incomplete. Missing info: {}",
                handler.getStep(),
                analysis.missingInfo());

        if (analysis.missingInfo() == null || analysis.missingInfo().isBlank()) {
            return handler.getSystemPrompt();
        }

        return """
                %s
                
                [SYSTEM NOTICE]
                The user has provided incomplete data.
                Current status: DATA INCOMPLETE.
                Please politely ask for the following:
                %s
                """.formatted(handler.getSystemPrompt(), analysis.missingInfo());
    }

    private SseEmitter streamResponse(UUID chatId,
                                      String userMessage,
                                      String systemPrompt) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content()
                .doOnNext(chunk -> sendChunk(emitter, chunk))
                .doOnError(error -> handleError(emitter, error))
                .doOnComplete(emitter::complete)
                .subscribe();

        return emitter;
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(chunk);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void handleError(SseEmitter emitter, Throwable error) {
        log.error("Streaming error", error);
        emitter.completeWithError(error);
    }

    private AbstractChatStepHandler resolveHandler(ChatFlowStep step) {
        return Optional.ofNullable(stepHandlers.get(step))
                .orElseThrow(() ->
                        new AppException("No handler found for step: " + step, 500));
    }

    private String buildHistoryForValidator(AiChat chat, String currentUserMessage) {

        List<ChatMessage> messages = Optional.ofNullable(chat.getMessages())
                .orElse(Collections.emptyList());

        int startIndex = Math.max(0, messages.size() - HISTORY_LIMIT);

        String history = messages.stream()
                .skip(startIndex)
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        return history + "\nUSER: " + currentUserMessage;
    }

    public String ats(String resumeId, String link) {
        log.debug("Attempting process ats for chatId={} and vacancy={}", resumeId, link);
        UserProfile user = securityService.findCurrentUser();
        Resume resume = resumeService.findById(resumeId);
        String jobDescription = jobParserService.parse(link);
        try {
            analyzerService.ats(resume, jobDescription);
        } catch (Exception e) {
            log.warn("Exception in ATS for resume: {}", resumeId, e);
            Map<String, Object> params = new HashMap<>();
            params.put("resumeId", resume.getId());
            params.put("status", "rejected");
            sendNotification(user.getEmail(), params, "resume_rejected");
            notificationService.sendNotification(NotificationDto.builder()
                    .engine(NotificationEngine.WS)
                    .parameters(Map.of("message", "Ошибка адаптации резюме!", "status", WsType.ERROR))
                    .receiver(user.getLogin())
                    .build());
        }
        return null;
    }

    public String ats(UUID chatId, String link) {
        log.debug("Attempting process ats for chatId={} and vacancy={}", chatId, link);
        Resume resume = resumeService.findByChatId(chatId);
        String jobDescription = jobParserService.parse(link);
        analyzerService.ats(resume, jobDescription);
        return null;
    }

    private void sendNotification(String email, Map<String, Object> params, String templateName) {
        notificationService.sendNotification(
                NotificationDto.builder()
                        .engine(NotificationEngine.EMAIL)
                        .receiver(email)
                        .parameters(params)
                        .templateName(templateName)
                        .build()
        );
    }
}
