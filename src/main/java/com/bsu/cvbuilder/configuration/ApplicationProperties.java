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

    @Getter
    @Setter
    public static class Security {
        private String decodeSignature;
        private String accessSecret;
        private String refreshSecret;
        private String accessLifetime;
        private String refreshLifetime;
    }
}