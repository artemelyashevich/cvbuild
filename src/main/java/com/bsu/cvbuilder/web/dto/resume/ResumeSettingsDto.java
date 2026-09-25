package com.bsu.cvbuilder.web.dto.resume;

public record ResumeSettingsDto(
        String name,
        String resumeTemplate,
        String ownerId,
        String ownerLogin
) {
}
