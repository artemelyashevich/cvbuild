package com.bsu.cvbuilder.domain.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.UUID;

@Builder
@Schema(description = "Data transfer object for sending requests to the AI service")
public record AiRequestDto(

        @Schema(
                description = "Unique identifier for the specific chat session",
                example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID chatId,

        @Schema(
                description = "Unique identifier of the user sending the message",
                example = "user_12345",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String userId,

        @Schema(
                description = "The actual text content or prompt to be processed by the AI",
                example = "Can you improve the summary section of my CV?",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String content
) {
}