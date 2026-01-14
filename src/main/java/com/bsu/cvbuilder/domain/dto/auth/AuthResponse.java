package com.bsu.cvbuilder.domain.dto.auth;

public record AuthResponse(
        String accessToken, String refreshToken
) {
}
