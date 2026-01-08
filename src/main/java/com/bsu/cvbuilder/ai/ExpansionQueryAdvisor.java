package com.bsu.cvbuilder.ai;

import com.bsu.cvbuilder.domain.AiTemplateMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;

import java.util.Map;

@Slf4j
@Builder
public record ExpansionQueryAdvisor(ChatClient chatClient, int order, double temperature,
                                    double topP) implements BaseAdvisor {

    public static final String ENRICHED_QUESTION = "ENRICHED_QUESTION";
    public static final String ORIGINAL_QUESTION = "ORIGINAL_QUESTION";

    private static final PromptTemplate systemTemplate = PromptTemplate.builder()
            .template(AiTemplateMessage.SYSTEM_INTERVIEWER.getMessage())
            .build();

    private static final PromptTemplate expansionTemplate = PromptTemplate.builder()
            .template(AiTemplateMessage.PROMPT_EXPANSION.getMessage())
            .build();

    public static ExpansionQueryAdvisorBuilder builder(
            ChatModel chatModel,
            double temperature,
            double topP
    ) {
        return new ExpansionQueryAdvisorBuilder()
                .chatClient(
                        ChatClient.builder(chatModel)
                                .defaultOptions(
                                        OllamaOptions.builder()
                                                .temperature(0.1)
                                                .topP(topP)
                                                .build()
                                )
                                .build()
                )
                .temperature(temperature)
                .topP(topP);
    }

    @Override
    @NonNull
    public ChatClientRequest before(ChatClientRequest chatClientRequest, @NonNull AdvisorChain advisorChain) {
        String originalQuestion = chatClientRequest.prompt().getUserMessage().getText();
        log.info("Expansion: original='{}'", originalQuestion);

        String enrichedQuestion = expand(originalQuestion);

        if (enrichedQuestion == null || enrichedQuestion.isBlank()) {
            log.warn("Expansion: failed expanding '{}', using original", originalQuestion);
            enrichedQuestion = originalQuestion;
        } else {
            log.info("Expansion: enriched='{}'", enrichedQuestion);
        }

        return chatClientRequest.mutate()
                .context(ORIGINAL_QUESTION, originalQuestion)
                .context(ENRICHED_QUESTION, enrichedQuestion)
                .build();
    }

    @Override
    @NonNull
    public ChatClientResponse after(@NonNull ChatClientResponse chatClientResponse, @NonNull AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    private String expand(String originalQuestion) {
        try {
            String rendered = expansionTemplate.render(Map.of("question", originalQuestion));
            return chatClient
                    .prompt()
                    .user(rendered)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Expansion: failed expand question '{}'", originalQuestion, e);
            return null;
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}