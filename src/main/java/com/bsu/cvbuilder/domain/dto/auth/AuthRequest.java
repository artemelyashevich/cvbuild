package com.bsu.cvbuilder.domain.dto.auth;

public record AuthRequest(
        String email,
        String password
) {
}
