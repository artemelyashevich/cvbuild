package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.entity.UserStats;
import com.bsu.cvbuilder.service.LockService;
import com.bsu.cvbuilder.repository.UserStatsRepository;
import com.bsu.cvbuilder.service.impl.UserStatsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStatsServiceImplTest {

    @Mock
    private UserStatsRepository userStatsRepository;
    @Mock
    private LockService lockService;

    @InjectMocks
    private UserStatsServiceImpl userStatsService;

    @BeforeEach
    void setUp() {
        lenient().when(lockService.withLock(anyString(), any())).thenAnswer(inv -> inv.<Supplier<?>>getArgument(1).get());
    }

    // --- save Tests ---

    @Test
    @DisplayName("save: should persist and return user stats")
    void save_ValidUserStats_ReturnsSavedStats() {
        // Arrange
        var inputStats = TestDataFactory.createStats("user-1", 10);
        when(userStatsRepository.save(inputStats)).thenReturn(inputStats);

        // Act
        var result = userStatsService.save(inputStats);

        // Assert
        assertAll(
                () -> assertEquals("user-1", result.getUserId()),
                () -> verify(userStatsRepository).save(inputStats)
        );
    }

    @Test
    @DisplayName("save: should return existing stats of the user instead of inserting a duplicate")
    void save_ExistingUserStats_ReturnsExistingWithoutInsert() {
        // Arrange
        var existing = TestDataFactory.createStats("user-1", 3);
        when(userStatsRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));

        // Act
        var result = userStatsService.save(TestDataFactory.createStats("user-1", 0));

        // Assert
        assertSame(existing, result);
        verify(userStatsRepository, never()).save(any());
    }

    // --- findByUserId Tests ---

    @Test
    @DisplayName("findByUserId: should return stats when user id exists")
    void findByUserId_ExistingId_ReturnsUserStats() {
        // Arrange
        var userId = "user-123";
        var expectedStats = TestDataFactory.createStats(userId, 5);
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(expectedStats));

        // Act
        var actualStats = userStatsService.findByUserId(userId);

        // Assert
        assertEquals(userId, actualStats.getUserId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-id", "non-existent"})
    @DisplayName("findByUserId: should create empty stats when none exist yet")
    void findByUserId_NonExistentId_CreatesStats(String userId) {
        // Arrange
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userStatsRepository.save(any(UserStats.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var stats = userStatsService.findByUserId(userId);

        // Assert
        assertAll(
                () -> assertEquals(userId, stats.getUserId()),
                () -> assertEquals(0L, stats.getTotalTokens()),
                () -> verify(userStatsRepository).save(any(UserStats.class))
        );
    }

    // --- incrementStats Tests ---

    @Test
    @DisplayName("incrementStats: should apply updater to existing stats and save")
    void incrementStats_ExistingUser_AppliesUpdateAndSaves() {
        // Arrange
        var userId = "user-1";
        var existingStats = TestDataFactory.createStats(userId, 10);
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.of(existingStats));

        // Mocking the behavior of an updater (e.g., incrementing total messages)
        Consumer<UserStats> updater = stats -> stats.setResumesCreated(stats.getResumesCreated() + 1);

        // Act
        userStatsService.incrementStats(userId, updater);

        // Assert
        var captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());

        assertEquals(11, captor.getValue().getResumesCreated());
        assertEquals(userId, captor.getValue().getUserId());
    }

    @Test
    @DisplayName("incrementStats: should create new stats if user id not found then save")
    void incrementStats_NewUser_InitializesAndSaves() {
        // Arrange
        var userId = "new-user";
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Consumer<UserStats> updater = stats -> stats.setResumesCreated(1);

        // Act
        userStatsService.incrementStats(userId, updater);

        // Assert
        var captor = ArgumentCaptor.forClass(UserStats.class);
        verify(userStatsRepository).save(captor.capture());

        var savedStats = captor.getValue();
        assertAll(
                () -> assertEquals(userId, savedStats.getUserId()),
                () -> assertEquals(1, savedStats.getResumesCreated())
        );
    }

    private static class TestDataFactory {
        static UserStats createStats(String userId, int messages) {
            return UserStats.builder()
                    .userId(userId)
                    .resumesCreated(messages)
                    .build();
        }
    }
}