package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.exception.AuthTokenException;
import com.bsu.cvbuilder.service.BlackListService;
import com.bsu.cvbuilder.service.impl.JwtServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ApplicationProperties.Security securityProps;

    @Mock
    private BlackListService blackListService;

    @InjectMocks
    private JwtServiceImpl jwtService;

    private static final String TEST_ACCESS_SECRET = "test-access-secret-key-at-least-32-chars-long";
    private static final String TEST_REFRESH_SECRET = "test-refresh-secret-key-at-least-32-chars-long";

    @BeforeEach
    void setUp() {
        // Arrange properties for @PostConstruct init()
        when(applicationProperties.getSecurity()).thenReturn(securityProps);
        when(securityProps.getAccessSecret()).thenReturn(TEST_ACCESS_SECRET);
        when(securityProps.getRefreshSecret()).thenReturn(TEST_REFRESH_SECRET);

        jwtService.init();
    }

    // --- extractLogin Tests ---

    @Test
    @DisplayName("extractLogin: should return subject login from valid token")
    void extractLogin_ValidToken_ReturnsLogin() {
        // Arrange
        var user = TestDataFactory.createSampleUser();
        when(securityProps.getAccessLifetime()).thenReturn("10000");
        var token = jwtService.generateToken(user, TokenType.ACCESS);

        // Act
        var login = jwtService.extractLogin(token, TokenType.ACCESS);

        // Assert
        assertEquals(user.getLogin(), login);
    }

    // --- extractRole Tests ---

    @Test
    @DisplayName("extractRole: should return mapped user role from claims")
    void extractRole_ValidToken_ReturnsRole() {
        // Arrange
        var user = TestDataFactory.createSampleUser();
        when(securityProps.getAccessLifetime()).thenReturn("10000");
        var token = jwtService.generateToken(user, TokenType.ACCESS);

        // Act
        var role = jwtService.extractRole(token, TokenType.ACCESS);

        // Assert
        assertEquals(user.getRole(), role);
    }

    // --- validateToken Tests ---

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("validateToken: should throw 401 AppException when token is null or empty")
    void validateToken_EmptyToken_ThrowsAppException(String token) {
        // Act & Assert
        var ex = assertThrows(AppException.class, () -> jwtService.validateToken(token, TokenType.ACCESS));
        assertEquals(401, ex.getStatusCode());
        assertEquals("Token is empty", ex.getMessage());
    }

    @Test
    @DisplayName("validateToken: should throw 401 when token is expired")
    void validateToken_ExpiredToken_ThrowsAppException() {
        // Arrange
        var key = Keys.hmacShaKeyFor(TEST_ACCESS_SECRET.getBytes());
        var expiredToken = Jwts.builder()
                .setSubject("user")
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 1 second ago
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Act & Assert
        var ex = assertThrows(AuthTokenException.class, () -> jwtService.validateToken(expiredToken, TokenType.ACCESS));
        assertEquals("Token expired", ex.getMessage());
    }

    @Test
    @DisplayName("validateToken: should throw 401 when token is tampered with")
    void validateToken_TamperedToken_ThrowsAppException() {
        // Arrange
        var user = TestDataFactory.createSampleUser();
        when(securityProps.getAccessLifetime()).thenReturn("10000");
        var validToken = jwtService.generateToken(user, TokenType.ACCESS);
        var tamperedToken = validToken + "extra_chars";

        // Act & Assert
        var ex = assertThrows(AppException.class, () -> jwtService.validateToken(tamperedToken, TokenType.ACCESS));
        assertTrue(ex.getMessage().contains("Invalid token"));
    }

    @Test
    @DisplayName("validateToken: should throw 401 when token is signed with wrong secret")
    void validateToken_WrongSecret_ThrowsAppException() {
        // Arrange
        var wrongSecret = "very-different-secret-key-that-should-fail";
        var key = Keys.hmacShaKeyFor(wrongSecret.getBytes());
        var tokenWithWrongSecret = Jwts.builder()
                .setSubject("user")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Act & Assert
        var ex = assertThrows(AppException.class, () -> jwtService.validateToken(tokenWithWrongSecret, TokenType.ACCESS));
        assertTrue(ex.getMessage().contains("Invalid token"));
    }

    private static class TestDataFactory {
        static UserProfile createSampleUser() {
            return UserProfile.builder()
                    .login("test_user")
                    .role(UserProfile.Role.USER)
                    .build();
        }
    }
}