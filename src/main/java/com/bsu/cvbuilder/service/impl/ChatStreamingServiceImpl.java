package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.service.ChatStreamingService;
import com.bsu.cvbuilder.service.PromptRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamingServiceImpl implements ChatStreamingService {

    private final ChatClient chatClient;
    private final PromptRegistryService promptRegistryService;

    @Override
    public Flux<String> process(AiRequestDto aiRequestDto) {
        log.debug("Attempting start streaming with chat: {}", aiRequestDto.chatId());
        StringBuilder fullResponseAccumulator = new StringBuilder();
        String systemPrompt = promptRegistryService.getPrompt("interviewer");

        Flux<String> res = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, aiRequestDto.chatId()))
                .system(systemPrompt)
                .user(aiRequestDto.content())
                .stream()
                .content()
                .doOnNext(fullResponseAccumulator::append)
                .concatWith(Flux.defer(() -> {
                    String finalFullText = fullResponseAccumulator.toString();

                    if (finalFullText.contains("COMPLETED")) {
                        log.debug("Attempting complete streaming");
                        String finalPrompt = promptRegistryService.getPrompt("final");
                        PromptTemplate promptTemplate = PromptTemplate.builder()
                                .template(finalPrompt)
                                .build();
                        Flux<String> result = chatClient.prompt()
                                .user(promptTemplate.render())
                                .stream()
                                .content();
                        log.info("COMPLETED");
                        return result;
                    }
                    return Flux.empty();
                }));
        log.info("Stream complete with chat: {}", aiRequestDto.chatId());
        return res;
    }
}
