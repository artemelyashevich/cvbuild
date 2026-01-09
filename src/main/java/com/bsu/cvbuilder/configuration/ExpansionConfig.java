package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.ai.ExpansionQueryAdvisor;
import com.bsu.cvbuilder.service.PromptRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ExpansionConfig {

    private final ApplicationProperties applicationProperties;
    private final PromptRegistryService promptRegistryService;

    @Bean
    public ExpansionQueryAdvisor expansionQueryAdvisor(ChatModel chatModel) {
        return ExpansionQueryAdvisor.builder(
                        chatModel,
                        applicationProperties.getChat(),
                        promptRegistryService
                )
                .build();
    }
}