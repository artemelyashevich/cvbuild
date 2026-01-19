package com.bsu.cvbuilder.domain.dto.auth;

import lombok.Builder;

@Builder
public record RegisterAuthDto(
        String firstName,
        String lastName,
        String email,
        String password
) {
}
