package com.bsu.cvbuilder.controller.integration;

import com.bsu.cvbuilder.AbstractTest;
import com.bsu.cvbuilder.controller.provider.AuthIntegrationTestData;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.RefreshRequest;
import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import com.bsu.cvbuilder.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest extends AbstractTest {

    @MockitoBean
    private AuthService authService;

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.controller.provider.AuthIntegrationTestData#registerIntegrationProvider")
    @DisplayName("POST /register: Should return 201 CREATED and valid JSON response")
    void register_ValidDto_Returns200AndAuthResponse(RegisterAuthDto request) {
        // Arrange
        var mockResponse = AuthIntegrationTestData.createMockResponse();
        when(authService.register(any(RegisterAuthDto.class))).thenReturn(mockResponse);

        // Act
        var response = restTemplate.postForEntity(
                "http://localhost:" + port + AuthIntegrationTestData.AUTH_BASE_URL + "/register",
                request,
                AuthResponse.class
        );

        // Assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().accessToken()).isEqualTo(mockResponse.accessToken()),
                () -> verify(authService).register(request)
        );
    }

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.controller.provider.AuthIntegrationTestData#loginIntegrationProvider")
    @DisplayName("POST /login: Should return 201 CREATED when credentials are valid")
    void login_ValidCredentials_Returns200AndAuthResponse(AuthRequest request) {
        // Arrange
        var mockResponse = AuthIntegrationTestData.createMockResponse();
        when(authService.authenticate(any(AuthRequest.class))).thenReturn(mockResponse);

        // Act
        var response = restTemplate.postForEntity(
                "http://localhost:" + port + AuthIntegrationTestData.AUTH_BASE_URL + "/login",
                request,
                AuthResponse.class
        );

        // Assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().refreshToken()).isEqualTo(mockResponse.refreshToken()),
                () -> verify(authService).authenticate(request)
        );
    }

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.controller.provider.AuthIntegrationTestData#refreshIntegrationProvider")
    @DisplayName("POST /refresh: Should return 201 CREATED and rotated tokens")
    void refresh_ValidToken_Returns200AndAuthResponse(RefreshRequest request) {
        // Arrange
        var mockResponse = AuthIntegrationTestData.createMockResponse();
        when(authService.refreshToken(any(RefreshRequest.class))).thenReturn(mockResponse);

        // Act
        var response = restTemplate.postForEntity(
                "http://localhost:" + port + AuthIntegrationTestData.AUTH_BASE_URL + "/refresh",
                request,
                AuthResponse.class
        );

        // Assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().accessToken()).isEqualTo(mockResponse.accessToken()),
                () -> verify(authService).refreshToken(request)
        );
    }
}