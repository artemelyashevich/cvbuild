package com.bsu.cvbuilder.web.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponseDto(
        String id,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        Boolean emailVerified,
        LocalDateTime lastLogin,
        LocalDateTime createdAt
) {
}
