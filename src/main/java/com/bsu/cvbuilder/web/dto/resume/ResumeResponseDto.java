package com.bsu.cvbuilder.web.dto.resume;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

public record ResumeResponseDto(
        String id,
        Map<String, Object> blocks,
        String chatId,
        ResumeSettingsDto resumeSettings,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,
        String atsId,
        boolean ats,
        Boolean generatedWithChat,
        Long version
) {
}
