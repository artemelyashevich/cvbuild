package com.bsu.cvbuilder.service.flow;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFlowService {

    private static final int HISTORY_LIMIT = 10;

    private final ChatClient chatClient;
    private final ChatService chatService;
    private final ResumeService resumeService;
    private final Map<ChatFlowStep, AbstractChatStepHandler> stepHandlers;

    @Autowired
    public ChatFlowService(ChatClient chatClient, ChatService chatService, ResumeService resumeService, List<AbstractChatStepHandler> handlers) {
        this.chatClient = chatClient;
        this.chatService = chatService;
        this.resumeService = resumeService;
        this.stepHandlers = handlers.stream()
                .collect(Collectors.toMap(AbstractChatStepHandler::getStep, Function.identity()));
    }

    public Resume extractFromChat(UUID chatId) {
        log.debug("Extract from chat with id {}", chatId);
        Resume resume = resumeService.findByChatId(chatId);
        log.info("Extracted from chat with id {}", chatId);
        return resume;
    }

    public SseEmitter processMessage(UUID chatId, String userMessage) {
        log.info("Processing Message from Chat Id: {}", chatId);

        AiChat chat = chatService.getChatById(chatId);
        SseEmitter emitter = new SseEmitter(0L);

        AbstractChatStepHandler currentHandler = stepHandlers.get(chat.getChatFlowStep());

        String validationHistory = buildHistoryForValidator(chat, userMessage);

        StepAnalysisResult analysis = currentHandler.analyzeCompletion(chatClient, validationHistory);

        String systemPrompt;

        if (analysis.completed()) {
            log.info("Step {} completed. Advancing to next step.", currentHandler.getStep());

            ChatFlowStep nextStep = currentHandler.getNextStep();
            if (nextStep.equals(ChatFlowStep.COMPLETED)) {
                chat.setFinished(true);
            }
            chat.setChatFlowStep(nextStep);
            chatService.saveAiChat(chat);

            AbstractChatStepHandler nextHandler = stepHandlers.get(nextStep);
            systemPrompt = nextHandler.getSystemPrompt();

            log.info("Transitioning to STEP: {}", nextStep);
        } else {
            log.info("Step {} incomplete. Missing: {}", currentHandler.getStep(), analysis.missingInfo());

            StringBuilder promptBuilder = new StringBuilder(currentHandler.getSystemPrompt());

            if (analysis.missingInfo() != null && !analysis.missingInfo().isEmpty()) {
                promptBuilder.append("\n\n[SYSTEM NOTICE]\n")
                        .append("The user has provided incomplete data.\n")
                        .append("Current status: DATA INCOMPLETE.\n")
                        .append("Please politely ask for the following: ")
                        .append(analysis.missingInfo());
            }
            systemPrompt = promptBuilder.toString();
        }

        chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    try {
                        emitter.send(chunk);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnError(emitter::completeWithError)
                .doOnComplete(emitter::complete)
                .subscribe();

        return emitter;

    }

    private String buildHistoryForValidator(AiChat chat, String currentUserMessage) {
        String history = chat.getMessages().stream()
                .skip(Math.max(0, chat.getMessages().size() - HISTORY_LIMIT))
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        return history + "\nUSER: " + currentUserMessage;
    }
}