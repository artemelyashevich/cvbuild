package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.AiChatRepository;
import com.bsu.cvbuilder.service.LockService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplOwnershipTest {

    private static final UUID CHAT_ID = UUID.randomUUID();
    private static final UserProfile ALICE = UserProfile.builder().id("alice-id").build();

    @Mock
    private SecurityService securityService;
    @Mock
    private AiChatRepository aiChatRepository;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private LockService lockService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any()))
                .thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    @Test
    @DisplayName("getOrCreateOwnChat: missing chat is created for the given user without the security context")
    void getOrCreateOwnChat_MissingChat_CreatesForUser() {
        when(aiChatRepository.findById(CHAT_ID)).thenReturn(Optional.empty());
        when(aiChatRepository.save(any(AiChat.class))).thenAnswer(inv -> inv.getArgument(0));

        AiChat chat = chatService.getOrCreateOwnChat(CHAT_ID, ALICE);

        assertEquals("alice-id", chat.getUserId());
        verifyNoInteractions(securityService);
    }

    @Test
    @DisplayName("getOrCreateOwnChat: chat of another user is rejected with 403")
    void getOrCreateOwnChat_ForeignChat_Throws403() {
        when(aiChatRepository.findById(CHAT_ID))
                .thenReturn(Optional.of(AiChat.builder().id(CHAT_ID).userId("bob-id").build()));

        AppException ex = assertThrows(AppException.class, () -> chatService.getOrCreateOwnChat(CHAT_ID, ALICE));

        assertEquals(403, ex.getStatusCode());
        verify(aiChatRepository, never()).save(any());
    }

    @Test
    @DisplayName("isAccessible: missing or own chat is accessible, foreign chat is not")
    void isAccessible_OwnershipMatrix() {
        UUID foreign = UUID.randomUUID();
        when(aiChatRepository.findById(CHAT_ID)).thenReturn(Optional.empty());
        when(aiChatRepository.findById(foreign))
                .thenReturn(Optional.of(AiChat.builder().id(foreign).userId("bob-id").build()));

        assertTrue(chatService.isAccessible(CHAT_ID, "alice-id"));
        assertFalse(chatService.isAccessible(foreign, "alice-id"));
    }
}
