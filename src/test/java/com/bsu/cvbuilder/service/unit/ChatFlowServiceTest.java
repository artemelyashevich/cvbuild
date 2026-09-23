package com.bsu.cvbuilder.service.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsu.cvbuilder.ai.TokenUsageAdvisor;
import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AnalyzerService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.JobParserService;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.TokenUsageService;
import com.bsu.cvbuilder.service.flow.chat.AbstractChatStepHandler;
import com.bsu.cvbuilder.service.flow.chat.ChatFlowService;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ChatFlowServiceTest {

    private static final UUID CHAT_ID = UUID.randomUUID();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;
    @Mock
    private ChatService chatService;
    @Mock
    private SecurityService securityService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private AbstractChatStepHandler startHandler;
    @Mock
    private TokenUsageService tokenUsageService;

    private ChatFlowService chatFlowService;

    @BeforeEach
    void setUp() {
        when(startHandler.getStep()).thenReturn(ChatFlowStep.START);
        chatFlowService = new ChatFlowService(chatClient, chatService,
                mock(ResumeService.class), mock(JobParserService.class), mock(AnalyzerService.class),
                mock(NotificationService.class), securityService, List.of(startHandler), applicationEventPublisher,
                tokenUsageService, mock(TokenUsageAdvisor.class));
        when(securityService.findCurrentUser()).thenReturn(UserProfile.builder().id("user-1").build());
    }

    @Test
    @DisplayName("streamMessage: chat owned by another user is rejected with 403")
    void streamMessage_ForeignChat_Throws403() {
        when(chatService.getChatById(CHAT_ID)).thenReturn(AiChat.builder().id(CHAT_ID).userId("user-2").build());

        AppException ex = assertThrows(AppException.class, () -> chatFlowService.streamMessage(CHAT_ID, "hi"));

        assertEquals(403, ex.getStatusCode());
        verifyNoInteractions(applicationEventPublisher, tokenUsageService);
    }

    @Test
    @DisplayName("streamMessage: own chat streams model tokens")
    @SuppressWarnings("unchecked")
    void streamMessage_OwnChat_StreamsTokens() {
        when(chatService.getChatById(CHAT_ID)).thenReturn(AiChat.builder().id(CHAT_ID).userId("user-1").build());
        when(startHandler.analyzeCompletion(any(), anyString())).thenReturn(new StepAnalysisResult(false, "", ""));
        when(startHandler.getSystemPrompt()).thenReturn("system");
        when(chatClient.prompt().advisors(any(Consumer.class)).system(anyString()).user(anyString()).stream().content())
                .thenReturn(Flux.just("Привет", "!"));

        List<String> tokens = chatFlowService.streamMessage(CHAT_ID, "hi").collectList().block();

        assertEquals(List.of("Привет", "!"), tokens);
        verify(applicationEventPublisher).publishEvent(any(Object.class));
        verify(tokenUsageService).checkLimit(any(UserProfile.class));
    }
}
