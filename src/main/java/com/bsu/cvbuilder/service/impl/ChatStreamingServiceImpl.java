package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.agreement.AgreementRequire;
import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.domain.event.UserGenerateNewMessageEvent;
import com.bsu.cvbuilder.service.ChatStreamingService;
import com.bsu.cvbuilder.service.PromptRegistryService;
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

    @Override
    // @AgreementRequire
    public Flux<String> process(AiRequestDto dto) {
        log.debug("Starting AI stream for chat: {}", dto.chatId());
        StringBuilder responseAccumulator = new StringBuilder();
        String systemPrompt = promptRegistryService.getPrompt(PROMPT_INTERVIEWER);
        applicationEventPublisher.publishEvent(UserGenerateNewMessageEvent.builder().userId(dto.userId()).build());
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, dto.chatId()))
                .system(systemPrompt)
                .user(dto.content())
                .stream()
                .content()
                .doOnNext(responseAccumulator::append)
                .concatWith(Flux.defer(() -> {
                    if (responseAccumulator.toString().contains(COMPLETED_SIGNAL)) {
                        return executeFinalStep(dto.chatId());
                    }
                    return Flux.empty();
                }))
                .doOnError(e -> log.error("Error during AI streaming for chat {}: {}", dto.chatId(), e.getMessage()))
                .doOnComplete(() -> log.info("Stream finished successfully for chat: {}", dto.chatId()));
    }

    private Flux<String> executeFinalStep(UUID chatId) {
        log.debug("Signal '{}' detected. Triggering final AI summary for chat: {}", COMPLETED_SIGNAL, chatId);

        String finalPrompt = promptRegistryService.getPrompt(PROMPT_FINAL);

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(finalPrompt)
                .stream()
                .content()
                .doOnSubscribe(s -> log.debug("Final summary stream started"));
    }
}