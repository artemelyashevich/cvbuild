package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.PromptRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final PromptRegistryService promptRegistryService;
    private final ApplicationProperties applicationProperties;

    @Override
    public String call(AiRequestDto aiRequestDto) {
        log.debug("Attempting call AI: {}", aiRequestDto.chatId());
        String prompt = promptRegistryService.getPrompt("interviewer");
        String response = chatClient.prompt()
                .advisors(
                        advisorSpec -> advisorSpec.param(
                                ChatMemory.CONVERSATION_ID,
                                aiRequestDto.chatId()
                        )
                )
                .system(s -> s.text(prompt))
                .user(u -> u.text(aiRequestDto.content()))
                .call()
                .content();

        log.info("Response from AI [INTERVIEWER] generated");
        if (response != null && response.contains("COMPLETED")) {
            log.debug("Response from AI [INTERVIEWER] generated: COMPLETED");
            String finalPrompt = promptRegistryService.getPrompt("final");
            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(finalPrompt)
                    .build();
            return chatClient.prompt()
                    .user(u -> u.text(promptTemplate.render()))
                    .call()
                    .content();
        }
        return response;
    }

    @Override
    public ChatClient.CallResponseSpec callExtractor(String history, UUID chatId) {
        log.debug("Attempting call AI: extractor");
        String extractorPrompt = promptRegistryService.getPrompt("extractor");
        ChatClient.CallResponseSpec spec = chatClient.prompt()
                .user(u -> u.text(extractorPrompt.formatted(history)))
                .options(OllamaOptions.builder()
                        .format("json")
                        .temperature(applicationProperties.getChat().getExtractionTemperature())
                        .numPredict(2000)
                        .build())
                .advisors(
                        advisorSpec -> advisorSpec.param(
                                ChatMemory.CONVERSATION_ID,
                                chatId
                        )
                )
                .call();
        log.info("Ai extracted");
        return spec;
    }

    @Override
    public String callExpand(String text) {
        log.debug("Attempting call AI: expand");
        String prompt = promptRegistryService.getPrompt("expansion");
        PromptTemplate expansionTemplate = PromptTemplate.builder()
                .template(prompt)
                .build();
        String rendered = expansionTemplate.render(Map.of("question", text));
        String response = chatClient
                .prompt()
                .user(rendered)
                .call()
                .content();
        log.info("AI expanded");
        return response;
    }
}
