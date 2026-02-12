package com.bsu.cvbuilder.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Refresh token request data")
public record RefreshRequest(

        @Schema(
                description = "JWT refresh token",
                example = "eyJpXVCJ9.eyJzdWIiOiIx2MjM5MDIyfQ.SfldQssw5c",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Refresh token is required")
        @Size(min = 20, max = 500, message = "Refresh token must be between 20 and 500 characters")
        String refreshToken
) {}