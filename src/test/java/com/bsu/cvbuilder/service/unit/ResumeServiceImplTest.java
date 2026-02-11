package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.ChatMessage;
import com.bsu.cvbuilder.domain.entity.MessageRole;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.impl.ResumeServiceImpl;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private AiService aiService;
    @Mock
    private ChatService chatService;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    // --- findAll Tests ---

    @Test
    @DisplayName("findAll: should return page of resumes and call count when list is full")
    void findAll_FullPage_ReturnsPopulatedPageAndCallsCount() {
        // Arrange
        var pageSize = 2;
        var pageable = PageRequest.of(0, pageSize);
        var resumes = List.of(new Resume(), new Resume());

        when(mongoTemplate.find(any(Query.class), eq(Resume.class))).thenReturn(resumes);
        when(mongoTemplate.count(any(Query.class), eq(Resume.class))).thenReturn(10L);

        // Act
        var result = resumeService.findAll(pageable);

        // Assert
        assertAll(
                () -> assertEquals(10, result.getTotalElements()),
                () -> assertEquals(2, result.getContent().size()),
                () -> verify(mongoTemplate).count(any(Query.class), eq(Resume.class))
        );
    }

    // --- findByChatId Tests ---

    @Test
    @DisplayName("findByChatId: should return existing resume from DB if present")
    void findByChatId_ResumeInDb_ReturnsExistingResume() {
        // Arrange
        var chatId = UUID.randomUUID();
        var existingResume = Resume.builder().chatId(chatId.toString()).build();
        when(mongoTemplate.findOne(any(Query.class), eq(Resume.class))).thenReturn(existingResume);

        // Act
        var result = resumeService.findByChatId(chatId);

        // Assert
        assertEquals(chatId.toString(), result.getChatId());
        verifyNoInteractions(aiService); // AI should not be called
    }

    @Test
    @DisplayName("findByChatId: should trigger AI generation if resume not found in DB")
    void findByChatId_ResumeMissing_TriggersAiGeneration() {
        // Arrange
        var chatId = UUID.randomUUID();
        var chatHistory = TestDataFactory.createChatWithMessages(chatId);
        var generatedResume = Resume.builder().id("gen-1").build();

        // Mock response spec for fluent AI call
        var responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(mongoTemplate.findOne(any(Query.class), eq(Resume.class))).thenReturn(null);
        when(chatService.getChatById(chatId)).thenReturn(chatHistory);
        when(aiService.callExtractor(anyString(), eq(chatId))).thenReturn(responseSpec);
        when(responseSpec.entity(any(BeanOutputConverter.class))).thenReturn(generatedResume);
        when(mongoTemplate.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = resumeService.findByChatId(chatId);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(chatId.toString(), result.getChatId()),
                () -> verify(aiService).callExtractor(contains("USER: Hello"), eq(chatId)),
                () -> verify(mongoTemplate).save(any(Resume.class))
        );
    }

    // --- findById Tests ---

    @Test
    @DisplayName("findById: should return resume when valid ID is provided")
    void findById_ValidId_ReturnsResume() {
        // Arrange
        var id = "res-123";
        var expected = Resume.builder().id(id).build();
        when(mongoTemplate.findById(id, Resume.class)).thenReturn(expected);

        // Act
        var result = resumeService.findById(id);

        // Assert
        assertEquals(id, result.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-id", "non-existent"})
    @DisplayName("findById: should throw 404 AppException when resume not found")
    void findById_InvalidId_ThrowsNotFoundException(String id) {
        // Arrange
        when(mongoTemplate.findById(id, Resume.class)).thenReturn(null);

        // Act & Assert
        var exception = assertThrows(AppException.class, () -> resumeService.findById(id));
        assertEquals(404, exception.getStatusCode());
    }

    // --- update Tests ---

    @Test
    @DisplayName("update: should update blocks and save when valid request")
    void update_ValidRequest_UpdatesAndSaves() {
        // Arrange
        var id = "id-1";
        var existingResume = Resume.builder().id(id).blocks(Map.of("bio", "Old")).build();
        var updateRequest = new UpdateResumeRequest(Map.of("bio", "New Bio"));

        when(mongoTemplate.findById(id, Resume.class)).thenReturn(existingResume);
        when(mongoTemplate.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = resumeService.update(id, updateRequest);

        // Assert
        var resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(mongoTemplate).save(resumeCaptor.capture());

        assertEquals("New Bio", resumeCaptor.getValue().getBlocks().get("bio"));
        assertEquals("New Bio", result.getBlocks().get("bio"));
    }

    // --- AI Error Scenarios (internal generateAndSave logic) ---

    @Test
    @DisplayName("findByChatId: should throw 500 when AI returns null entity")
    void findByChatId_AiReturnsNull_ThrowsAppException() {
        // Arrange
        var chatId = UUID.randomUUID();
        var responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(mongoTemplate.findOne(any(Query.class), eq(Resume.class))).thenReturn(null);
        when(chatService.getChatById(chatId)).thenReturn(TestDataFactory.createChatWithMessages(chatId));
        when(aiService.callExtractor(anyString(), eq(chatId))).thenReturn(responseSpec);
        when(responseSpec.entity(any(BeanOutputConverter.class))).thenReturn(null);

        // Act & Assert
        var ex = assertThrows(AppException.class, () -> resumeService.findByChatId(chatId));
        assertEquals(500, ex.getStatusCode());
    }

    @Test
    @DisplayName("findByChatId: should throw 500 when AI service throws exception")
    void findByChatId_AiServiceFails_ThrowsAppException() {
        // Arrange
        var chatId = UUID.randomUUID();
        when(mongoTemplate.findOne(any(Query.class), eq(Resume.class))).thenReturn(null);
        when(chatService.getChatById(chatId)).thenReturn(TestDataFactory.createChatWithMessages(chatId));
        when(aiService.callExtractor(anyString(), eq(chatId))).thenThrow(new RuntimeException("AI Down"));

        // Act & Assert
        var ex = assertThrows(AppException.class, () -> resumeService.findByChatId(chatId));
        assertEquals(500, ex.getStatusCode());
    }

    private static class TestDataFactory {
        static AiChat createChatWithMessages(UUID chatId) {
            var msg = ChatMessage.builder()
                    .role(MessageRole.USER)
                    .content("Hello, here is my experience")
                    .build();
            return AiChat.builder()
                    .id(chatId)
                    .messages(List.of(msg))
                    .build();
        }
    }
}