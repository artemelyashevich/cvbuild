package com.bsu.cvbuilder.entity.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPreferences {

    @Builder.Default
    private String language = "ru";

    @Builder.Default
    private Locale locale = Locale.forLanguageTag("ru-RU");

    @Builder.Default
    private String timezone = "Europe/Moscow";

    @Builder.Default
    private String theme = "light";

    @Builder.Default
    private Boolean emailNotifications = true;

    @Builder.Default
    private Boolean productUpdates = true;

    @Builder.Default
    private Boolean jobRecommendations = true;

    @Builder.Default
    private String defaultTemplateId = "default-modern";

    @Builder.Default
    private Boolean autoSave = true;

    @Builder.Default
    private Integer autoSaveInterval = 30;
}
