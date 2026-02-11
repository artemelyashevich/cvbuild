package com.bsu.cvbuilder.controller.provider;

import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.RefreshRequest;
import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class AuthTestData {

    public static AuthResponse createAuthResponse() {
        return new AuthResponse("access-token-123", "refresh-token-456");
    }

    public static Stream<Arguments> registerProvider() {
        return Stream.of(
                Arguments.of(new RegisterAuthDto("user@test.com", "password123", "John", "Doe")),
                Arguments.of(new RegisterAuthDto("admin@test.com", "securePass!", "Admin", "User"))
        );
    }

    public static Stream<Arguments> loginProvider() {
        return Stream.of(
                Arguments.of(new AuthRequest("user@test.com", "password123")),
                Arguments.of(new AuthRequest("admin@test.com", "securePass!"))
        );
    }

    public static Stream<Arguments> refreshProvider() {
        return Stream.of(
                Arguments.of(new RefreshRequest("refresh-token-456"))
        );
    }
}