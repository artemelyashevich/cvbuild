package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.entity.chat.AiChat;
import com.bsu.cvbuilder.repository.AiChatRepository;
import com.bsu.cvbuilder.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Disabled
@Deprecated
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private AiChatRepository aiChatRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    // --- createAiChat Tests ---

    @Test
    @DisplayName("createAiChat: should save and return a new AiChat")
    void createAiChat_ValidId_ReturnsSavedChat() {
        // Arrange
        var chatId = UUID.randomUUID();
        var expectedChat = TestDataFactory.createChat(chatId);
        when(aiChatRepository.save(any(AiChat.class))).thenReturn(expectedChat);

        // Act
        var actualChat = chatService.createAiChat(chatId);

        // Assert
        assertAll(
                () -> assertEquals(chatId, actualChat.getId()),
                () -> verify(aiChatRepository).save(any(AiChat.class))
        );
    }

    // --- getChatById Tests ---

    @Test
    @DisplayName("getChatById: should return chat from repository when it exists")
    void getChatById_ExistingChat_ReturnsChatFromRepo() {
        // Arrange
        var chatId = UUID.randomUUID();
        var existingChat = TestDataFactory.createChat(chatId);
        when(aiChatRepository.findById(chatId)).thenReturn(Optional.of(existingChat));

        // Act
        var actualChat = chatService.getChatById(chatId);

        // Assert
        assertAll(
                () -> assertEquals(chatId, actualChat.getId()),
                () -> verify(aiChatRepository, never()).save(any(AiChat.class))
        );
    }

    @Test
    @DisplayName("getChatById: should create and return new chat when not found in repository")
    void getChatById_NonExistentChat_CreatesAndReturnsNewChat() {
        // Arrange
        var chatId = UUID.randomUUID();
        var newChat = TestDataFactory.createChat(chatId);
        
        when(aiChatRepository.findById(chatId)).thenReturn(Optional.empty());
        when(aiChatRepository.save(any(AiChat.class))).thenReturn(newChat);

        // Act
        var actualChat = chatService.getChatById(chatId);

        // Assert
        assertAll(
                () -> assertEquals(chatId, actualChat.getId()),
                () -> verify(aiChatRepository).findById(chatId),
                () -> verify(aiChatRepository).save(any(AiChat.class))
        );
    }

    // --- saveAiChat Tests ---

    @Test
    @DisplayName("saveAiChat: should persist provided chat object")
    void saveAiChat_ValidChat_ReturnsSavedChat() {
        // Arrange
        var chatId = UUID.randomUUID();
        var chatToSave = TestDataFactory.createChat(chatId);
        when(aiChatRepository.save(chatToSave)).thenReturn(chatToSave);

        // Act
        var savedChat = chatService.saveAiChat(chatToSave);

        // Assert
        assertAll(
                () -> assertEquals(chatId, savedChat.getId()),
                () -> verify(aiChatRepository).save(chatToSave)
        );
    }

    private static class TestDataFactory {
        static AiChat createChat(UUID id) {
            return AiChat.builder()
                    .id(id)
                    .build();
        }
    }
}