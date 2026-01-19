package com.bsu.cvbuilder.domain.dto.auth;

public record RegisterAuthDto(
        String firstName,
        String lastName,
        String email,
        String password
) {
}
