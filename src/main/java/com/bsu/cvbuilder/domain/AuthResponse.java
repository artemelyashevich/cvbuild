package com.bsu.cvbuilder.domain;

public record AuthResponse(
        String accessToken, String refreshToken
) {
}
