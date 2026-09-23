package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.ai.TokenUsageAdvisor;
import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.UserGenerateNewMessageEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.TokenUsageService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.service.impl.ChatStreamingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatStreamingServiceImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;
    @Mock
    private PromptRegistryService promptRegistryService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private TokenUsageService tokenUsageService;
    @Mock
    private TokenUsageAdvisor tokenUsageAdvisor;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private ChatService chatService;

    @Test
    @DisplayName("process: user comes from the authenticated login, client-supplied userId is ignored")
    @SuppressWarnings("unchecked")
    void process_SpoofedUserId_UsesAuthenticatedUser() {
        var service = new ChatStreamingServiceImpl(chatClient, promptRegistryService, applicationEventPublisher,
                tokenUsageService, tokenUsageAdvisor, userProfileService, chatService);
        UserProfile user = UserProfile.builder().id("real-user").login("alice").build();
        when(userProfileService.findByLogin("alice")).thenReturn(user);
        when(promptRegistryService.getPrompt(anyString())).thenReturn("prompt");
        when(chatClient.prompt().advisors(any(Consumer.class)).system(anyString()).user(anyString()).stream().content())
                .thenReturn(Flux.just("ok"));

        service.process(new AiRequestDto(UUID.randomUUID(), "victim-user", "hi"), "alice").blockLast();

        verify(chatService).getOrCreateOwnChat(any(UUID.class), eq(user));
        verify(tokenUsageService).checkLimit(user);
        ArgumentCaptor<UserGenerateNewMessageEvent> event = ArgumentCaptor.forClass(UserGenerateNewMessageEvent.class);
        verify(applicationEventPublisher).publishEvent(event.capture());
        assertEquals("real-user", event.getValue().getUserId());
    }

    @Test
    @DisplayName("process: foreign chat is rejected with 403 before any model call")
    void process_ForeignChat_Throws403() {
        var service = new ChatStreamingServiceImpl(chatClient, promptRegistryService, applicationEventPublisher,
                tokenUsageService, tokenUsageAdvisor, userProfileService, chatService);
        UUID chatId = UUID.randomUUID();
        when(userProfileService.findByLogin("alice")).thenReturn(UserProfile.builder().id("real-user").build());
        when(chatService.getOrCreateOwnChat(eq(chatId), any(UserProfile.class)))
                .thenThrow(new AppException("denied", 403));

        AppException ex = assertThrows(AppException.class,
                () -> service.process(new AiRequestDto(chatId, null, "hi"), "alice"));

        assertEquals(403, ex.getStatusCode());
        verifyNoInteractions(chatClient, tokenUsageService, applicationEventPublisher);
    }
}
