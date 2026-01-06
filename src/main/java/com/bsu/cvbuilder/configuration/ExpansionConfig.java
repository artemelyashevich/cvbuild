package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.ai.ExpansionQueryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExpansionConfig {

    @Bean
    ExpansionQueryAdvisor expansionQueryAdvisor(
            ChatModel chatModel,
            @Value("${expansion.advisor.temperature:0.1}") double temperature,
            @Value("${expansion.advisor.top-p:0.4}") double topP
    ) {
        return ExpansionQueryAdvisor.builder(
                        chatModel,
                        temperature,
                        topP
                )
                .order(1)
                .build();
    }
}