package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.ai.TokenUsageAdvisor;
import com.bsu.cvbuilder.annotation.agreement.AgreementRequire;
import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.UserGenerateNewMessageEvent;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ChatStreamingService;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.TokenUsageService;
import com.bsu.cvbuilder.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamingServiceImpl implements ChatStreamingService {

    private static final String PROMPT_INTERVIEWER = "interviewer";
    private static final String PROMPT_FINAL = "final";
    private static final String COMPLETED_SIGNAL = "COMPLETED";

    private final ChatClient chatClient;
    private final PromptRegistryService promptRegistryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TokenUsageService tokenUsageService;
    private final TokenUsageAdvisor tokenUsageAdvisor;
    private final UserProfileService userProfileService;
    private final ChatService chatService;

    @Override
    // @AgreementRequire
    @Monitored(value = "calling_ai_workflow", context = "ws")
    public Flux<String> process(AiRequestDto dto, String login) {
        log.debug("Starting AI stream for chat: {}", dto.chatId());
        StringBuilder responseAccumulator = new StringBuilder();
        String systemPrompt = promptRegistryService.getPrompt(PROMPT_INTERVIEWER);
        UserProfile user = userProfileService.findByLogin(login);
        // Created here with the known user: STOMP handler threads have no security context,
        // so the chat memory advisor must never be the one creating it
        chatService.getOrCreateOwnChat(dto.chatId(), user);
        tokenUsageService.checkLimit(user);
        applicationEventPublisher.publishEvent(UserGenerateNewMessageEvent.builder().userId(user.getId()).build());
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, dto.chatId())
                        .param(TokenUsageAdvisor.USER_ID, user.getId())
                        .advisors(tokenUsageAdvisor))
                .system(systemPrompt)
                .user(dto.content())
                .stream()
                .content()
                .doOnNext(responseAccumulator::append)
                .concatWith(Flux.defer(() -> {
                    if (responseAccumulator.toString().contains(COMPLETED_SIGNAL)) {
                        return executeFinalStep(dto.chatId(), user.getId());
                    }
                    return Flux.empty();
                }))
                .doOnError(e -> log.error("Error during AI streaming for chat {}: {}", dto.chatId(), e.getMessage()))
                .doOnComplete(() -> log.info("Stream finished successfully for chat: {}", dto.chatId()));
    }

    @Monitored(value = "calling_ai_workflow_final", context = "ws")
    private Flux<String> executeFinalStep(UUID chatId, String userId) {
        log.debug("Signal '{}' detected. Triggering final AI summary for chat: {}", COMPLETED_SIGNAL, chatId);

        String finalPrompt = promptRegistryService.getPrompt(PROMPT_FINAL);

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TokenUsageAdvisor.USER_ID, userId)
                        .advisors(tokenUsageAdvisor))
                .user(finalPrompt)
                .stream()
                .content()
                .doOnSubscribe(s -> log.debug("Final summary stream started"));
    }
}