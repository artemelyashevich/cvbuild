package com.bsu.cvbuilder.ai;

import com.bsu.cvbuilder.domain.event.TokenUsageEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUsageAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String USER_ID = "token_usage_user_id";

    private final ApplicationEventPublisher eventPublisher;
    private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

    @Override
    @NonNull
    public ChatClientResponse adviseCall(@NonNull ChatClientRequest request, @NonNull CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        usageOf(response).ifPresent(usage ->
                publish(request, valueOf(usage.getPromptTokens()), valueOf(usage.getCompletionTokens())));
        return response;
    }

    @Override
    @NonNull
    public Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request, @NonNull StreamAdvisorChain chain) {
        // Ollama reports token counts only in the final chunk; keep the last non-empty one
        AtomicReference<Usage> lastUsage = new AtomicReference<>();
        StringBuffer streamedText = new StringBuffer();
        return chain.nextStream(request)
                .doOnNext(response -> {
                    usageOf(response).ifPresent(lastUsage::set);
                    textOf(response).ifPresent(streamedText::append);
                })
                .doFinally(signal -> recordStreamUsage(request, lastUsage.get(), streamedText));
    }

    // A cancelled (barge-in, disconnect) or failed stream never receives the final chunk,
    // so whatever the model already produced is charged by estimate
    private void recordStreamUsage(ChatClientRequest request, Usage usage, StringBuffer streamedText) {
        try {
            if (usage != null) {
                publish(request, valueOf(usage.getPromptTokens()), valueOf(usage.getCompletionTokens()));
            } else if (!streamedText.isEmpty()) {
                publish(request,
                        tokenCountEstimator.estimate(request.prompt().getContents()),
                        tokenCountEstimator.estimate(streamedText.toString()));
            }
        } catch (Exception e) {
            log.error("Failed to record token usage", e);
        }
    }

    private static Optional<Usage> usageOf(ChatClientResponse response) {
        return Optional.ofNullable(response.chatResponse())
                .map(chatResponse -> chatResponse.getMetadata().getUsage())
                .filter(usage -> usage.getTotalTokens() != null && usage.getTotalTokens() > 0);
    }

    private static Optional<String> textOf(ChatClientResponse response) {
        return Optional.ofNullable(response.chatResponse())
                .map(ChatResponse::getResult)
                .map(generation -> generation.getOutput().getText());
    }

    private void publish(ChatClientRequest request, long promptTokens, long completionTokens) {
        Object userId = request.context().get(USER_ID);
        if (userId == null) {
            log.warn("Token usage advisor called without {} param, usage is not recorded", USER_ID);
            return;
        }
        eventPublisher.publishEvent(new TokenUsageEvent(userId.toString(), promptTokens, completionTokens));
    }

    private static long valueOf(Integer tokens) {
        return tokens == null ? 0 : tokens;
    }

    @Override
    @NonNull
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
