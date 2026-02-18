package com.bsu.cvbuilder.service.flow;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.ChatMessage;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatFlowService {

    private static final int HISTORY_LIMIT = 10;
    private static final long SSE_TIMEOUT = Duration.ofMinutes(5).toMillis();

    private final ChatClient chatClient;
    private final ChatService chatService;
    private final ResumeService resumeService;
    private final Map<ChatFlowStep, AbstractChatStepHandler> stepHandlers;

    public ChatFlowService(ChatClient chatClient,
                           ChatService chatService,
                           ResumeService resumeService,
                           List<AbstractChatStepHandler> handlers) {
        this.chatClient = chatClient;
        this.chatService = chatService;
        this.resumeService = resumeService;
        this.stepHandlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AbstractChatStepHandler::getStep,
                        Function.identity()
                ));
    }

    public Resume extractFromChat(UUID chatId) {
        log.debug("Extracting resume from chat {}", chatId);
        return resumeService.findByChatId(chatId);
    }

    public SseEmitter processMessage(UUID chatId, String userMessage) {

        log.info("Processing message for chatId={}", chatId);

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

        log.info("Step {} incomplete. Missing info: {}",
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
}
