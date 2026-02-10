package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.domain.event.UserCreatedEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.UserProfileRepository;
import com.bsu.cvbuilder.service.impl.UserProfileServiceImpl;
import com.bsu.cvbuilder.service.mapper.UserMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    // --- findByEmail Tests ---

    @Test
    @DisplayName("findByEmail: should return profile when user exists")
    void findByEmail_ExistingEmail_ReturnsUserProfile() {
        // Arrange
        var email = "test@example.com";
        var expectedUser = TestDataFactory.createSampleUser(email, "test_login");
        when(userProfileRepository.findByEmail(email)).thenReturn(Optional.of(expectedUser));

        // Act
        var actualUser = userProfileService.findByEmail(email);

        // Assert
        assertAll(
                () -> assertEquals(expectedUser.getEmail(), actualUser.getEmail()),
                () -> assertEquals(expectedUser.getLogin(), actualUser.getLogin())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown@mail.com", "empty@mail.com"})
    @DisplayName("findByEmail: should throw AppException 404 when user not found")
    void findByEmail_NonExistentEmail_ThrowsAppException(String email) {
        // Arrange
        when(userProfileRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> userProfileService.findByEmail(email));
        assertEquals(404, exception.getStatusCode());
        assertTrue(exception.getMessage().contains(email));
    }

    // --- findByLogin Tests ---

    @Test
    @DisplayName("findByLogin: should return profile when login exists")
    void findByLogin_ExistingLogin_ReturnsUserProfile() {
        // Arrange
        var login = "admin_user";
        var expectedUser = TestDataFactory.createSampleUser("admin@mail.com", login);
        when(userProfileRepository.findByLogin(login)).thenReturn(Optional.of(expectedUser));

        // Act
        var actualUser = userProfileService.findByLogin(login);

        // Assert
        assertEquals(login, actualUser.getLogin());
    }

    // --- findById Tests ---

    @Test
    @DisplayName("findById: should return profile when ID exists")
    void findById_ExistingId_ReturnsUserProfile() {
        // Arrange
        var id = "uuid-123";
        var expectedUser = UserProfile.builder().id(id).email("user@mail.com").build();
        when(userProfileRepository.findById(id)).thenReturn(Optional.of(expectedUser));

        // Act
        var actualUser = userProfileService.findById(id);

        // Assert
        assertEquals(id, actualUser.getId());
    }

    // --- existsByEmail Tests ---

    @ParameterizedTest
    @CsvSource({
            "exists@mail.com, true",
            "new@mail.com, false"
    })
    @DisplayName("existsByEmail: should return expected boolean result")
    void existsByEmail_StateUnderTest_ReturnsExpectedResult(String email, boolean expectedResult) {
        // Arrange
        when(userProfileRepository.existsByEmail(email)).thenReturn(expectedResult);

        // Act
        var actualResult = userProfileService.existsByEmail(email);

        // Assert
        assertEquals(expectedResult, actualResult);
    }

    // --- create Tests ---

    @Test
    @DisplayName("create: should save user and publish UserCreatedEvent")
    void create_ValidUserProfile_SavesAndPublishesEvent() {
        // Arrange
        var inputUser = TestDataFactory.createSampleUser("new@mail.com", "new_user");
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(inputUser);

        // Act
        var result = userProfileService.create(inputUser);

        // Assert
        var eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(userProfileRepository, times(1)).save(inputUser);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        assertEquals(inputUser.getEmail(), eventCaptor.getValue().getUser().getEmail());
        assertEquals(inputUser, result);
    }

    // --- login Tests ---

    @Test
    @DisplayName("login: should update lastLogin for existing user")
    void login_ExistingUser_UpdatesLastLogin() {
        // Arrange
        var login = "existing_user";
        var user = TestDataFactory.createSampleUser("test@mail.com", login);
        when(userProfileRepository.findByLogin(login)).thenReturn(Optional.of(user));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        var result = userProfileService.login(login);

        // Assert
        assertNotNull(result.getLastLogin());
    }

    @Test
    @DisplayName("login: should create new profile if user does not exist")
    void login_NewUser_CreatesNewProfile() {
        // Arrange
        var login = "new_user";
        when(userProfileRepository.findByLogin(login)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        var result = userProfileService.login(login);

        // Assert
        assertAll(
                () -> assertEquals(login, result.getLogin()),
                () -> assertNotNull(result.getLastLogin())
        );
        verify(userProfileRepository, atLeastOnce()).save(any(UserProfile.class));
    }

    // --- update Tests ---

    @Test
    @Disabled
    @Deprecated
    @DisplayName("update: should update fields and save when ID exists")
    void update_ExistingId_UpdatesFieldsAndSaves() {
        // Arrange
        var userId = "1";
        var existingUser = TestDataFactory.createSampleUser("old@mail.com", "old_login");
        existingUser.setId(userId);

        var updateRequest = UserProfile.builder()
                .id(userId)
                .firstName("NewName")
                .lastName("NewLastName")
                .lastLogin(LocalDateTime.MAX)
                .build();

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        userMapper.updateEntity(updateRequest, existingUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        var result = userProfileService.update(updateRequest);

        // Assert
        assertAll(
                () -> assertEquals("NewName", result.getFirstName()),
                () -> assertEquals("NewLastName", result.getLastName()),
                () -> assertEquals(LocalDateTime.MAX, result.getLastLogin())
        );
        verify(userProfileRepository).save(existingUser);
    }

    @Test
    @DisplayName("update: should throw AppException 404 when updating non-existent user")
    void update_NonExistentId_ThrowsAppException() {
        // Arrange
        var profile = UserProfile.builder().id("invalid").build();
        when(userProfileRepository.findById("invalid")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> userProfileService.update(profile));
    }

    private static class TestDataFactory {
        static UserProfile createSampleUser(String email, String login) {
            return UserProfile.builder()
                    .email(email)
                    .login(login)
                    .firstName("First")
                    .lastName("Last")
                    .build();
        }
    }
}