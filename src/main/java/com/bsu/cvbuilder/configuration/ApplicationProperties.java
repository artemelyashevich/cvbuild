package com.bsu.cvbuilder.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Security security;
    private Chat chat;
    private Prompt prompt;

    @Getter
    @Setter
    public static class Security {
        private String decodeSignature;
        private String accessSecret;
        private String refreshSecret;
        private String accessLifetime;
        private String refreshLifetime;
    }

    @Getter
    @Setter
    public static class Chat {
        private Integer maxMessages;
        private Double temperature;
        private Double topp;
        private Double expansionTopp;
        private Double expansionTemperature;
        private Double extractionTemperature;
        private Integer memoryMaxMessages;
        private String stopCondition;
    }

    @Getter
    @Setter
    public static class Prompt {
        private String expansion;
        private String extractor;
        private String finalPhase;
        private String interviewer;
    }
}