package com.bsu.cvbuilder.domain.dto.auth;

public record ResetPasswordDto(
        String oldPassword,
        String newPassword,
        String confirmedNewPassword
) {
}
