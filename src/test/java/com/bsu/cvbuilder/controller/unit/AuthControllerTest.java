package com.bsu.cvbuilder.controller.unit;

import com.bsu.cvbuilder.controller.provider.AuthTestData;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.RefreshRequest;
import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import com.bsu.cvbuilder.service.AuthService;
import com.bsu.cvbuilder.web.controller.rest.AuthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.controller.provider.AuthTestData#registerProvider")
    @DisplayName("Register: Valid DTOs should return expected AuthResponse and trigger service")
    void register_ValidRequest_ReturnsAuthResponse(RegisterAuthDto request) {
        // Arrange
        var expectedResponse = AuthTestData.createAuthResponse();
        when(authService.register(request)).thenReturn(expectedResponse);

        // Act
        var actualResponse = authController.register(request);

        // Assert
        assertAll(
                () -> assertEquals(expectedResponse.getAccessToken(), actualResponse.getAccessToken()),
                () -> assertEquals(expectedResponse.getRefreshToken(), actualResponse.getRefreshToken()),
                () -> verify(authService).register(request)
        );
    }

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.controller.provider.AuthTestData#loginProvider")
    @DisplayName("Login: Valid credentials should return expected AuthResponse and trigger service")
    void login_ValidRequest_ReturnsAuthResponse(AuthRequest request) {
        // Arrange
        var expectedResponse = AuthTestData.createAuthResponse();
        when(authService.authenticate(request)).thenReturn(expectedResponse);

        // Act
        var actualResponse = authController.login(request);

        // Assert
        assertAll(
                () -> assertEquals(expectedResponse.getAccessToken(), actualResponse.getAccessToken()),
                () -> verify(authService).authenticate(request)
        );
    }

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.controller.provider.AuthTestData#refreshProvider")
    @DisplayName("Refresh: Valid token should return new pair of tokens and trigger service")
    void refresh_ValidRequest_ReturnsAuthResponse(RefreshRequest request) {
        // Arrange
        var expectedResponse = AuthTestData.createAuthResponse();
        when(authService.refreshToken(request)).thenReturn(expectedResponse);

        // Act
        var actualResponse = authController.refresh(request);

        // Assert
        assertAll(
                () -> assertEquals(expectedResponse.getAccessToken(), actualResponse.getAccessToken()),
                () -> verify(authService).refreshToken(request)
        );
    }

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.controller.provider.AuthTestData#loginProvider")
    @DisplayName("Login: Service exceptions should propagate to the controller")
    void login_ServiceThrowsException_PropagatesException(AuthRequest request) {
        // Arrange
        var errorMessage = "Invalid credentials";
        when(authService.authenticate(request)).thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        var exception = assertThrows(RuntimeException.class, () -> authController.login(request));
        assertEquals(errorMessage, exception.getMessage());
        verify(authService).authenticate(request);
    }
}