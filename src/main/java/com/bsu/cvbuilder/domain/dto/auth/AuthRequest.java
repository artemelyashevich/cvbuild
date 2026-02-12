package com.bsu.cvbuilder.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Authentication request data")
public record AuthRequest(

        @Schema(
                description = "User's email address",
                example = "john.doe@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Schema(
                description = "User's password",
                example = "SecurePass123!",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 4,
                maxLength = 100,
                format = "password"
        )
        @NotBlank(message = "Password is required")
        @Size(min = 4, max = 100, message = "Password must be between 8 and 100 characters")
        String password
) {}