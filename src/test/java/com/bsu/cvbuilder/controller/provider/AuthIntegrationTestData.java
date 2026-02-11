package com.bsu.cvbuilder.controller.provider;

import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.RefreshRequest;
import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class AuthIntegrationTestData {

    public static final String AUTH_BASE_URL = "/api/v1/auth";

    public static AuthResponse createMockResponse() {
        return new AuthResponse("access-token-abc", "refresh-token-xyz");
    }

    public static Stream<Arguments> registerIntegrationProvider() {
        return Stream.of(
                Arguments.of(new RegisterAuthDto("new-user@test.com", "pass123", "John", "Doe"))
        );
    }

    public static Stream<Arguments> loginIntegrationProvider() {
        return Stream.of(
                Arguments.of(new AuthRequest("existing@test.com", "correct-password"))
        );
    }

    public static Stream<Arguments> refreshIntegrationProvider() {
        return Stream.of(
                Arguments.of(new RefreshRequest("valid-refresh-token"))
        );
    }
}