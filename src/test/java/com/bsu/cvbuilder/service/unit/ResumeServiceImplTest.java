package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.ChatMessage;
import com.bsu.cvbuilder.domain.entity.MessageRole;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.ResumeNotificationEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.LimitService;
import com.bsu.cvbuilder.service.LockService;
import com.bsu.cvbuilder.service.SecurityService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    @Mock
    private SecurityService securityService;
    @Mock
    private LockService lockService;
    @Mock
    private LimitService limitService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private TransactionTemplate transactionTemplate;

    private static final UserProfile CURRENT_USER = UserProfile.builder().id("user-1").login("alice").email("alice@mail.test").build();

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

        when(securityService.findCurrentUser()).thenReturn(UserProfile.builder().id("user-1").build());
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
        when(chatService.getOwnChat(chatId)).thenReturn(AiChat.builder().id(chatId).userId("user-1").build());
        when(mongoTemplate.findOne(any(Query.class), eq(Resume.class))).thenReturn(existingResume);

        // Act
        var result = resumeService.findByChatId(chatId);

        // Assert
        assertEquals(chatId.toString(), result.getChatId());
        verifyNoInteractions(aiService); // AI should not be called
    }

    // --- findById Tests ---

    @Test
    @DisplayName("findById: should return resume when valid ID is provided")
    void findById_ValidId_ReturnsResume() {
        // Arrange
        var id = "res-123";
        var expected = ownedResume(id, "user-1");
        when(mongoTemplate.findById(id, Resume.class)).thenReturn(expected);
        when(securityService.findCurrentUser()).thenReturn(CURRENT_USER);

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
        var existingResume = ownedResume(id, "user-1");
        existingResume.setBlocks(Map.of("bio", "Old"));
        when(securityService.findCurrentUser()).thenReturn(CURRENT_USER);
        var updateRequest = new UpdateResumeRequest(Map.of("bio", "New Bio"));

        when(lockService.withLock(anyString(), any())).thenAnswer(inv -> inv.<Supplier<?>>getArgument(1).get());
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
        when(chatService.getOwnChat(chatId)).thenReturn(TestDataFactory.createChatWithMessages(chatId));

        // Act & Assert
        var ex = assertThrows(AppException.class, () -> resumeService.findByChatId(chatId));
        assertEquals(404, ex.getStatusCode());
    }

    @Test
    @DisplayName("findByChatId: should throw 500 when AI service throws exception")
    void findByChatId_AiServiceFails_ThrowsAppException() {
        // Arrange
        var chatId = UUID.randomUUID();
        when(mongoTemplate.findOne(any(Query.class), eq(Resume.class))).thenReturn(null);
        when(chatService.getOwnChat(chatId)).thenReturn(TestDataFactory.createChatWithMessages(chatId));

        // Act & Assert
        var ex = assertThrows(AppException.class, () -> resumeService.findByChatId(chatId));
        assertEquals(404, ex.getStatusCode());
    }

    // --- Ownership ---

    @Test
    @DisplayName("findById: resume of another user is rejected with 403")
    void findById_ForeignResume_Throws403() {
        when(mongoTemplate.findById("res-1", Resume.class)).thenReturn(ownedResume("res-1", "user-2"));
        when(securityService.findCurrentUser()).thenReturn(CURRENT_USER);

        var ex = assertThrows(AppException.class, () -> resumeService.findById("res-1"));

        assertEquals(403, ex.getStatusCode());
    }

    @Test
    @DisplayName("findById: legacy chat resume without ownerId is available to the chat owner only")
    void findById_LegacyChatResume_OwnershipByChat() {
        var chatId = UUID.randomUUID();
        var legacy = Resume.builder().id("res-1").chatId(chatId.toString()).build();
        when(mongoTemplate.findById("res-1", Resume.class)).thenReturn(legacy);
        when(securityService.findCurrentUser()).thenReturn(CURRENT_USER);

        when(chatService.isOwnedBy(chatId, "user-1")).thenReturn(true);
        assertSame(legacy, resumeService.findById("res-1"));

        when(chatService.isOwnedBy(chatId, "user-1")).thenReturn(false);
        var ex = assertThrows(AppException.class, () -> resumeService.findById("res-1"));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    @DisplayName("tryFindById: missing resume is empty instead of 404")
    void tryFindById_Missing_ReturnsEmpty() {
        when(mongoTemplate.findById("res-1", Resume.class)).thenReturn(null);

        assertTrue(resumeService.tryFindById("res-1").isEmpty());
    }

    @Test
    @DisplayName("findByChatId: chat of another user is rejected before any lookup or AI call")
    void findByChatId_ForeignChat_Throws403() {
        var chatId = UUID.randomUUID();
        when(chatService.getOwnChat(chatId)).thenThrow(new AppException("denied", 403));

        var ex = assertThrows(AppException.class, () -> resumeService.findByChatId(chatId));

        assertEquals(403, ex.getStatusCode());
        verifyNoInteractions(mongoTemplate, aiService);
    }

    // --- Generation ---

    @Test
    @DisplayName("findByChatId: generated resume gets the current user as owner and a GENERATED notification")
    void findByChatId_Generated_SetsOwnerAndNotifies() {
        var chatId = UUID.randomUUID();
        stubGeneration(chatId);
        var extracted = Resume.builder().blocks(Map.of("bio", "x")).build();
        var extractorSpec = mock(ChatClient.CallResponseSpec.class);
        var expansionSpec = mock(ChatClient.CallResponseSpec.class);
        when(aiService.callExtractor(anyString(), eq(chatId))).thenReturn(extractorSpec);
        when(extractorSpec.entity(any(BeanOutputConverter.class))).thenReturn(extracted);
        when(aiService.callExpansion(extracted)).thenReturn(expansionSpec);
        when(expansionSpec.entity(any(BeanOutputConverter.class))).thenReturn(extracted);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        when(mongoTemplate.save(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId("res-new");
            return r;
        });

        var result = resumeService.findByChatId(chatId);

        assertAll(
                () -> assertEquals("user-1", result.getResumeSettings().getOwnerId()),
                () -> assertEquals("alice", result.getResumeSettings().getOwnerLogin()),
                () -> assertEquals(chatId.toString(), result.getChatId()),
                () -> verify(limitService).check("user-1", LimitType.RESUME_GENERATE, 5),
                () -> verify(applicationEventPublisher).publishEvent(new ResumeNotificationEvent(
                        "alice", "alice@mail.test", "res-new", ResumeNotificationEvent.Kind.GENERATED))
        );
    }

    @Test
    @DisplayName("findByChatId: AI failure surfaces as AppException 500 (not NPE) and a GENERATION_FAILED notification")
    void findByChatId_AiFails_Throws500AndNotifies() {
        var chatId = UUID.randomUUID();
        stubGeneration(chatId);
        when(aiService.callExtractor(anyString(), eq(chatId))).thenThrow(new IllegalStateException("model down"));

        var ex = assertThrows(AppException.class, () -> resumeService.findByChatId(chatId));

        assertEquals(500, ex.getStatusCode());
        verify(applicationEventPublisher).publishEvent(new ResumeNotificationEvent(
                "alice", "alice@mail.test", null, ResumeNotificationEvent.Kind.GENERATION_FAILED));
        verify(mongoTemplate, never()).save(any(Resume.class));
    }

    private void stubGeneration(UUID chatId) {
        var chat = TestDataFactory.createChatWithMessages(chatId);
        chat.setFinished(true);
        when(chatService.getOwnChat(chatId)).thenReturn(chat);
        when(mongoTemplate.findOne(any(Query.class), eq(Resume.class))).thenReturn(null);
        when(securityService.findCurrentUser()).thenReturn(CURRENT_USER);
        when(lockService.withLock(anyString(), any())).thenAnswer(inv -> inv.<Supplier<?>>getArgument(1).get());
    }

    private static Resume ownedResume(String id, String ownerId) {
        return Resume.builder()
                .id(id)
                .resumeSettings(Resume.ResumeSettings.builder().ownerId(ownerId).build())
                .build();
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