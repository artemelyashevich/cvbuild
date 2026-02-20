package com.bsu.cvbuilder.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record Verify2faRequest(
        @NotBlank String email,
        @NotBlank String code
) {}