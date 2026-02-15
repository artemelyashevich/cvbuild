package com.bsu.cvbuilder.service.flow;

import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.service.PromptRegistryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StepValidator {

    private final ChatClient.Builder chatClientBuilder;
    private BeanOutputConverter<StepAnalysisResult> beanOutputConverter;
    private final PromptRegistryService promptRegistryService;
    private ChatClient validationClient;

    @PostConstruct
    public void init() {
        beanOutputConverter = new BeanOutputConverter<>(StepAnalysisResult.class);

        validationClient = chatClientBuilder
                .defaultOptions(
                        OllamaOptions.builder()
                                .temperature(0.0)
                                .topP(1.0)
                                .repeatPenalty(1.1)
                                .build()
                )
                .build();
    }

    public StepAnalysisResult validate(String history, String requirements) {

        String systemPrompt = promptRegistryService.getPrompt("validator").formatted(requirements, beanOutputConverter.getFormat());

        String userPrompt = """
            History:
            ---------------------
            %s
            ---------------------
            """.formatted(history);

        return validationClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(beanOutputConverter);
    }
}
