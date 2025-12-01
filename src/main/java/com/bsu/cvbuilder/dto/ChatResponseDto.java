package com.bsu.cvbuilder.dto;

import lombok.Builder;

@Builder
public record ChatResponseDto(
        String message, boolean isOption
) {
}
