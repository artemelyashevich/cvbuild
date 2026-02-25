package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.entity.UserStats;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.UserStatsRepository;
import com.bsu.cvbuilder.service.impl.UserStatsServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStatsServiceImplTest {

    @Mock
    private UserStatsRepository userStatsRepository;

    @InjectMocks
    private UserStatsServiceImpl userStatsService;

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
    @DisplayName("findByUserId: should throw AppException with 404 when not found")
    void findByUserId_NonExistentId_ThrowsAppException(String userId) {
        // Arrange
        when(userStatsRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(AppException.class, () -> userStatsService.findByUserId(userId));
        assertEquals(404, exception.getStatusCode());
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