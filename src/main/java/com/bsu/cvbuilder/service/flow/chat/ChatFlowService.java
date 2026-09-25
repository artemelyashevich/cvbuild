package com.bsu.cvbuilder.service.flow.chat;

import com.bsu.cvbuilder.ai.TokenUsageAdvisor;
import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.ChatMessage;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.UserGenerateNewMessageEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.TokenUsageService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatFlowService {

    private static final int HISTORY_LIMIT = 15;
    private static final long SSE_TIMEOUT = Duration.ofMinutes(5).toMillis();

    private final ChatClient chatClient;
    private final ChatService chatService;
    private final SecurityService securityService;
    private final Map<ChatFlowStep, AbstractChatStepHandler> stepHandlers;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TokenUsageService tokenUsageService;
    private final TokenUsageAdvisor tokenUsageAdvisor;

    public ChatFlowService(ChatClient chatClient,
                           ChatService chatService,
                           SecurityService securityService,
                           List<AbstractChatStepHandler> handlers, ApplicationEventPublisher applicationEventPublisher,
                           TokenUsageService tokenUsageService, TokenUsageAdvisor tokenUsageAdvisor) {
        this.chatClient = chatClient;
        this.chatService = chatService;
        this.securityService = securityService;
        this.stepHandlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AbstractChatStepHandler::getStep,
                        Function.identity()
                ));
        this.applicationEventPublisher = applicationEventPublisher;
        this.tokenUsageService = tokenUsageService;
        this.tokenUsageAdvisor = tokenUsageAdvisor;
    }

    public Flux<String> streamMessage(UUID chatId, String userMessage) {
        UserProfile userProfile = securityService.findCurrentUser();
        String systemPrompt = prepareSystemPrompt(userProfile, chatId, userMessage);

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TokenUsageAdvisor.USER_ID, userProfile.getId())
                        .advisors(tokenUsageAdvisor))
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content();
    }

    @Transactional
    public SseEmitter processMessage(UUID chatId, String userMessage) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        Disposable subscription = streamMessage(chatId, userMessage)
                .subscribe(
                        chunk -> sendChunk(emitter, chunk),
                        error -> handleError(emitter, error),
                        emitter::complete
                );

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(subscription::dispose);
        emitter.onError(e -> subscription.dispose());

        return emitter;
    }

    private String prepareSystemPrompt(UserProfile userProfile, UUID chatId, String userMessage) {
        log.info("Processing message for chatId={}", chatId);
        AiChat chat = chatService.getOrCreateOwnChat(chatId, userProfile);

        tokenUsageService.checkLimit(userProfile);

        applicationEventPublisher.publishEvent(new UserGenerateNewMessageEvent(userProfile.getId()));

        AbstractChatStepHandler currentHandler = resolveHandler(chat.getChatFlowStep());

        StepAnalysisResult analysis = analyzeStep(chat, userMessage, currentHandler);

        return resolveSystemPrompt(chat, currentHandler, analysis);
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
}
