package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.ai.TokenUsageAdvisor;
import com.bsu.cvbuilder.domain.event.TokenUsageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenUsageAdvisorTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CallAdvisorChain callChain;
    @Mock
    private StreamAdvisorChain streamChain;

    private static ChatClientRequest request(Map<String, Object> context) {
        return new ChatClientRequest(new Prompt("hi"), context);
    }

    private static ChatClientResponse response(String text, int prompt, int completion) {
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(text))))
                .metadata(ChatResponseMetadata.builder().usage(new DefaultUsage(prompt, completion)).build())
                .build();
        return new ChatClientResponse(chatResponse, Map.of());
    }

    @Test
    @DisplayName("adviseStream: publishes usage from the final chunk once the stream completes")
    void adviseStream_FinalChunkUsage_PublishesOnce() {
        var advisor = new TokenUsageAdvisor(eventPublisher);
        var request = request(Map.of(TokenUsageAdvisor.USER_ID, "user-1"));
        when(streamChain.nextStream(request)).thenReturn(Flux.just(
                response("При", 0, 0),
                response("вет", 0, 0),
                response("", 120, 35)
        ));

        advisor.adviseStream(request, streamChain).blockLast();

        verify(eventPublisher).publishEvent(new TokenUsageEvent("user-1", 120, 35));
    }

    @Test
    @DisplayName("adviseStream: cancelled stream publishes estimated usage of what was generated")
    void adviseStream_CancelledAfterOutput_PublishesEstimate() {
        var advisor = new TokenUsageAdvisor(eventPublisher);
        var request = request(Map.of(TokenUsageAdvisor.USER_ID, "user-1"));
        when(streamChain.nextStream(request)).thenReturn(Flux.just(
                response("Расскажите о своём опыте работы", 0, 0),
                response("", 120, 35)
        ));

        advisor.adviseStream(request, streamChain).take(1).blockLast();

        ArgumentCaptor<TokenUsageEvent> event = ArgumentCaptor.forClass(TokenUsageEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals("user-1", event.getValue().userId());
        assertTrue(event.getValue().promptTokens() > 0);
        assertTrue(event.getValue().completionTokens() > 0);
    }

    @Test
    @DisplayName("adviseStream: stream failing after output publishes estimated usage")
    void adviseStream_ErrorAfterOutput_PublishesEstimate() {
        var advisor = new TokenUsageAdvisor(eventPublisher);
        var request = request(Map.of(TokenUsageAdvisor.USER_ID, "user-1"));
        when(streamChain.nextStream(request)).thenReturn(Flux.concat(
                Flux.just(response("Привет", 0, 0)),
                Flux.error(new IllegalStateException("model down"))
        ));

        assertThrows(IllegalStateException.class, () -> advisor.adviseStream(request, streamChain).blockLast());

        verify(eventPublisher).publishEvent(any(TokenUsageEvent.class));
    }

    @Test
    @DisplayName("adviseStream: stream cancelled before any output publishes nothing")
    void adviseStream_CancelledBeforeOutput_PublishesNothing() {
        var advisor = new TokenUsageAdvisor(eventPublisher);
        var request = request(Map.of(TokenUsageAdvisor.USER_ID, "user-1"));
        when(streamChain.nextStream(request)).thenReturn(Flux.never());

        advisor.adviseStream(request, streamChain).subscribe().dispose();

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("adviseCall: publishes usage, skips when user id param is missing")
    void adviseCall_WithAndWithoutUserId() {
        var advisor = new TokenUsageAdvisor(eventPublisher);
        when(callChain.nextCall(any())).thenReturn(response("ok", 10, 5));

        advisor.adviseCall(request(Map.of(TokenUsageAdvisor.USER_ID, "user-1")), callChain);
        advisor.adviseCall(request(Map.of()), callChain);

        verify(eventPublisher, times(1)).publishEvent(new TokenUsageEvent("user-1", 10, 5));
        verifyNoMoreInteractions(eventPublisher);
    }
}
