package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.SecureDataRepository;
import com.bsu.cvbuilder.service.*;
import com.bsu.cvbuilder.service.impl.SecurityServiceImpl;
import com.bsu.cvbuilder.util.OtpKeyUtil;
import com.bsu.cvbuilder.util.SecretDecodeUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Disabled
@ExtendWith(MockitoExtension.class)
class SecurityServiceImplTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private OtpService otpService;
    @Mock
    private JwtService jwtService;
    @Mock
    private SecureDataRepository secureDataRepository;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ApplicationProperties.Security securityProps;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecureDataService secureDataService;

    @InjectMocks
    private SecurityServiceImpl securityService;

    private static final String DECODE_SIG = "test-sig";

    // --- findCurrentUser Tests ---

    @Test
    @DisplayName("findCurrentUser: should return profile when context contains valid login")
    void findCurrentUser_ValidAuthentication_ReturnsUserProfile() {
        try (MockedStatic<SecurityContextHolder> contextMock = mockStatic(SecurityContextHolder.class)) {
            // Arrange
            var login = "test_user";
            var user = TestDataFactory.createSampleUser(login);
            var auth = TestDataFactory.createMockAuth(login);
            var securityContext = mock(SecurityContext.class);

            contextMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(userProfileService.findByLogin(login)).thenReturn(user);

            // Act
            var result = securityService.findCurrentUser();

            // Assert
            assertEquals(login, result.getLogin());
        }
    }

    // --- authenticate Tests ---

    @Test
    @DisplayName("authenticate: should return AuthResponse and send notification if email missing")
    void authenticate_NoEmailInProfile_ReturnsResponseAndNotifies() {
        try (MockedStatic<SecretDecodeUtil> secretMock = mockStatic(SecretDecodeUtil.class)) {
            // Arrange
            var login = "user1";
            var user = TestDataFactory.createSampleUser(login);
            user.setEmail(null); // Trigger notification
            var auth = TestDataFactory.createMockAuth(login);
            var secureData = SecureData.builder().refreshTokenEncoded("encoded").build();

            when(userProfileService.login(login)).thenReturn(user);
            when(secureDataService.prepareData(user)).thenReturn(secureData);
            when(jwtService.generateToken(user, TokenType.ACCESS)).thenReturn("access-token");
            when(applicationProperties.getSecurity()).thenReturn(securityProps);
            when(securityProps.getDecodeSignature()).thenReturn(DECODE_SIG);
            secretMock.when(() -> SecretDecodeUtil.decode("encoded", DECODE_SIG)).thenReturn("decoded-refresh");

            // Act
            var result = securityService.authenticate(auth);

            // Assert
            assertAll(
                    () -> assertEquals("access-token", result.getAccessToken()),
                    () -> assertEquals("decoded-refresh", result.getRefreshToken()),
                    () -> verify(notificationService).sendNotification(any(NotificationDto.class))
            );
        }
    }

    // --- checkOtp Tests ---

    @Test
    @DisplayName("checkOtp: should verify email when OTP matches")
    void checkOtp_CorrectOtp_VerifiesEmail() {
        try (MockedStatic<SecurityContextHolder> contextMock = mockStatic(SecurityContextHolder.class)) {
            // Arrange
            var login = "user";
            var email = "test@mail.com";
            var user = TestDataFactory.createSampleUser(login);
            user.setEmail(email);
            var auth = TestDataFactory.createMockAuth(login);
            var securityContext = mock(SecurityContext.class);

            contextMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(userProfileService.findByLogin(login)).thenReturn(user);
            when(otpService.create(user, OtpKeyUtil.EMAIL_KEY)).thenReturn("123456");

            // Act
            securityService.checkOtp("123456");

            // Assert
            assertTrue(user.getEmailVerified());
            verify(userProfileService).update(user);
            verify(notificationService).sendNotification(any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"000000", "incorrect"})
    @DisplayName("checkOtp: should throw 401 AppException when OTP mismatch")
    void checkOtp_IncorrectOtp_ThrowsAppException(String wrongOtp) {
        try (MockedStatic<SecurityContextHolder> contextMock = mockStatic(SecurityContextHolder.class)) {
            // Arrange
            var login = "user";
            var user = TestDataFactory.createSampleUser(login);
            user.setEmail("test@mail.com");

            var auth = TestDataFactory.createMockAuth(login);

            var securityContext = mock(SecurityContext.class);
            contextMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            when(securityContext.getAuthentication()).thenReturn(auth);
            when(userProfileService.findByLogin(login)).thenReturn(user);
            when(otpService.create(user, OtpKeyUtil.EMAIL_KEY)).thenReturn("123456");

            // Act & Assert
            var ex = assertThrows(AppException.class, () -> securityService.checkOtp(wrongOtp));
            assertEquals(401, ex.getStatusCode());
        }
    }

    // --- refreshAccessToken Tests (moved -> AuthService)---

//    @Test
//    @DisplayName("refreshAccessToken: should return new access token for valid refresh token")
//    void refreshAccessToken_ValidToken_ReturnsNewTokens() {
//        try (MockedStatic<SecretDecodeUtil> secretMock = mockStatic(SecretDecodeUtil.class)) {
//            // Arrange
//            var login = "user@mail.com";
//            var token = "valid-refresh";
//            var user = TestDataFactory.createSampleUser(login);
//            var secureData = SecureData.builder().refreshTokenEncoded("encoded").build();
//
//            when(jwtService.extractLogin(token, TokenType.REFRESH)).thenReturn(login);
//            when(userProfileService.findByEmail(login)).thenReturn(user);
//            when(secureDataRepository.findByUserId(user.getId())).thenReturn(Optional.of(secureData));
//            when(applicationProperties.getSecurity()).thenReturn(securityProps);
//            when(securityProps.getDecodeSignature()).thenReturn(DECODE_SIG);
//            secretMock.when(() -> SecretDecodeUtil.encode(token, DECODE_SIG)).thenReturn("encoded");
//            when(jwtService.generateToken(user, TokenType.ACCESS)).thenReturn("new-access");
//
//            // Act
//            var result = securityService.refreshAccessToken(token);
//
//            // Assert
//            assertAll(
//                    () -> assertEquals("new-access", result.accessToken()),
//                    () -> assertEquals(token, result.refreshToken()),
//                    () -> verify(jwtService).validateToken(token, TokenType.REFRESH)
//            );
//        }
//    }
//
//    @Test
//    @DisplayName("refreshAccessToken: should throw 401 if refresh token does not match DB")
//    void refreshAccessToken_TokenMismatch_ThrowsAppException() {
//        try (MockedStatic<SecretDecodeUtil> secretMock = mockStatic(SecretDecodeUtil.class)) {
//            // Arrange
//            var login = "user@mail.com";
//            var token = "mismatch-token";
//            var user = TestDataFactory.createSampleUser(login);
//            var secureData = SecureData.builder().refreshTokenEncoded("encoded-in-db").build();
//
//            when(jwtService.extractLogin(token, TokenType.REFRESH)).thenReturn(login);
//            when(userProfileService.findByEmail(login)).thenReturn(user);
//            when(secureDataRepository.findByUserId(user.getId())).thenReturn(Optional.of(secureData));
//            when(applicationProperties.getSecurity()).thenReturn(securityProps);
//            when(securityProps.getDecodeSignature()).thenReturn(DECODE_SIG);
//            secretMock.when(() -> SecretDecodeUtil.encode(token, DECODE_SIG)).thenReturn("encoded-mismatch");
//
//            // Act & Assert
//            assertThrows(AppException.class, () -> securityService.refreshAccessToken(token));
//        }
//    }

    // --- Test Data Factory ---

    private static class TestDataFactory {
        static UserProfile createSampleUser(String login) {
            return UserProfile.builder()
                    .id(UUID.randomUUID().toString())
                    .login(login)
                    .emailVerified(false)
                    .build();
        }

        static Authentication createMockAuth(String login) {
            var principal = mock(DefaultOAuth2User.class);
            when(principal.getAttribute("login")).thenReturn(login);
            return new UsernamePasswordAuthenticationToken(principal, null);
        }
    }
}