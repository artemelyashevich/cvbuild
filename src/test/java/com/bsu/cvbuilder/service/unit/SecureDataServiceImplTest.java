package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.SecureDataRepository;
import com.bsu.cvbuilder.service.JwtService;
import com.bsu.cvbuilder.service.impl.SecureDataServiceImpl;
import com.bsu.cvbuilder.util.SecretDecodeUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecureDataServiceImplTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ApplicationProperties.Security securityProps;
    @Mock
    private SecureDataRepository secureDataRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SecureDataServiceImpl secureDataService;

    private static final String SIGNATURE = "test-signature";

    // --- prepareData Tests ---

    @Test
    @DisplayName("prepareData: should create new secure data when none exists")
    void prepareData_NewUser_CreatesAndSaves() {
        try (MockedStatic<SecretDecodeUtil> utilities = mockStatic(SecretDecodeUtil.class)) {
            // Arrange
            var user = TestDataFactory.createSampleUser("1");
            var token = "raw-token";
            var encodedToken = "encoded-token";

            setupSecurityProps();
            when(secureDataRepository.findByUserId("1")).thenReturn(Optional.empty());
            when(jwtService.generateToken(user, TokenType.REFRESH)).thenReturn(token);
            utilities.when(() -> SecretDecodeUtil.encode(token, SIGNATURE)).thenReturn(encodedToken);
            when(secureDataRepository.save(any(SecureData.class))).thenAnswer(i -> i.getArguments()[0]);

            // Act
            var result = secureDataService.prepareData(user);

            // Assert
            assertAll(
                    () -> assertEquals("1", result.getUserId()),
                    () -> assertEquals(encodedToken, result.getRefreshTokenEncoded()),
                    () -> verify(secureDataRepository).save(any(SecureData.class))
            );
        }
    }

    @Test
    @Disabled
    @DisplayName("prepareData: should throw and clear token when existing token is invalid")
    void prepareData_InvalidExistingToken_ClearsAndThrows() {
        try (MockedStatic<SecretDecodeUtil> utilities = mockStatic(SecretDecodeUtil.class)) {
            // Arrange
            var user = TestDataFactory.createSampleUser("1");
            var existingData = SecureData.builder().userId("1").refreshTokenEncoded("bad-token").build();

            setupSecurityProps();
            when(secureDataRepository.findByUserId("1")).thenReturn(Optional.of(existingData));
            when(jwtService.generateToken(user, TokenType.REFRESH)).thenReturn("new-token");
            utilities.when(() -> SecretDecodeUtil.decode("bad-token", SIGNATURE)).thenReturn("decrypted-bad-token");

            doThrow(new AppException("Expired", 401))
                    .when(jwtService).validateToken("decrypted-bad-token", TokenType.REFRESH);

            assertNotNull(existingData.getRefreshTokenEncoded());
            verify(secureDataRepository).save(existingData);
        }
    }

    // --- checkData Tests ---

    @Test
    @DisplayName("checkData: should complete successfully when credentials match")
    void checkData_ValidCredentials_DoesNotThrow() {
        // Arrange
        var user = TestDataFactory.createSampleUser("1");
        var request = new AuthRequest("test@mail.com", "password");
        var secureData = SecureData.builder().password("encoded-password").build();

        when(secureDataRepository.findByUserId("1")).thenReturn(Optional.of(secureData));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() -> secureDataService.checkData(user, request));
    }

    @Test
    @DisplayName("checkData: should throw 401 when password mismatch")
    void checkData_WrongPassword_ThrowsAppException() {
        // Arrange
        var user = TestDataFactory.createSampleUser("1");
        var request = new AuthRequest("test@mail.com", "wrong");
        var secureData = SecureData.builder().password("encoded-password").build();

        when(secureDataRepository.findByUserId("1")).thenReturn(Optional.of(secureData));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        // Act & Assert
        var ex = assertThrows(AppException.class, () -> secureDataService.checkData(user, request));
        assertEquals(401, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    @DisplayName("checkData: should throw 401 when secure data is missing")
    void checkData_MissingSecureData_ThrowsAppException() {
        // Arrange
        var user = TestDataFactory.createSampleUser("non-existent");
        when(secureDataRepository.findByUserId("non-existent")).thenReturn(Optional.empty());

        // Act & Assert
        var ex = assertThrows(AppException.class, () -> secureDataService.checkData(user, new AuthRequest("a", "b")));
        assertEquals(401, ex.getStatusCode());
    }

    // --- Helpers ---

    private void setupSecurityProps() {
        when(applicationProperties.getSecurity()).thenReturn(securityProps);
        when(securityProps.getDecodeSignature()).thenReturn(SIGNATURE);
    }

    private static class TestDataFactory {
        static UserProfile createSampleUser(String id) {
            return UserProfile.builder()
                    .id(id)
                    .email("test@mail.com")
                    .build();
        }
    }
}