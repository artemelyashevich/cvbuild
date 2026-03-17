package com.bsu.cvbuilder.web.dto.user;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponseDto(
        String id,
        String email,
        String login,
        String firstName,
        String lastName,
        String avatarUrl,
        Boolean emailVerified,
        Boolean secondAuthPhase,
        UserProfile.Role role,
        LocalDateTime lastLogin,
        LocalDateTime createdAt
) {
}
