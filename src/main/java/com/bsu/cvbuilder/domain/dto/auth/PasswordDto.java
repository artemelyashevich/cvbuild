package com.bsu.cvbuilder.domain.dto.auth;

public record PasswordDto(
        String newPassword,
        String confirmedPassword
) {
}
