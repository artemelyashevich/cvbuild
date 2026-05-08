package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.ai.ExpansionQueryAdvisor;
import com.bsu.cvbuilder.ai.MongoChatMemory;
import com.bsu.cvbuilder.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.context.annotation.Primary;


@Configuration
@RequiredArgsConstructor
public class AiConfiguration {

    private final ChatService aiService;
    private final ExpansionQueryAdvisor expansionQueryAdvisor;
    private final ApplicationProperties applicationProperties;

    @Bean
    @Primary
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultAdvisors(
                        expansionQueryAdvisor,
                        addMongoChatMemoryAdvisor(2),
                        SimpleLoggerAdvisor.builder().order(3).build()
                )
                .defaultOptions(
                        OllamaOptions.builder()
                                .temperature(applicationProperties.getChat().getTemperature())
                                .topP(applicationProperties.getChat().getTopp())
                                .build()
                )
                .build();
    }

    @Bean("expansionChatClient")
    public ChatClient expansionChatClient(ChatClient.Builder builder) {
        return builder
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().order(1).build()
                )
                .defaultOptions(
                        OllamaOptions.builder()
                                .temperature(applicationProperties.getChat().getTemperature())
                                .topP(applicationProperties.getChat().getTopp())
                                .build()
                )
                .build();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(applicationProperties.getChat().getMemoryMaxMessages())
                .build();
    }

    private Advisor addMongoChatMemoryAdvisor(int order) {
        return MessageChatMemoryAdvisor.builder(getMongoChatMemory())
                .order(order)
                .build();
    }

    private ChatMemory getMongoChatMemory() {
        return MongoChatMemory.builder()
                .maxMessages(applicationProperties.getChat().getMemoryMaxMessages())
                .chatService(aiService)
                .build();
    }
}
