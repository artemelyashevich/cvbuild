package com.bsu.cvbuilder.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Security security;
    private Chat chat;
    private Prompt prompt;
    private Cache cache;
    private Limit limit;
    private Analyzer analyzer;
    private Twilio twilio;
    private Telegram telegram;
    private String volkModel;

    @Getter
    @Setter
    public static class Telegram {
        private String token;
        private String chatId;
        private Boolean enabled;
        private String url;
    }

    @Getter
    @Setter
    public static class Analyzer {
        private List<String> trustUrls;
        private String userAgent;
    }

    @Getter
    @Setter
    public static class Limit {
        private Integer maxMessages;
        private Integer messagesBanDuration;
        private Duration banDuration;

        public Limit(Integer maxMessages, Integer messagesBanDuration) {
            this.maxMessages = maxMessages;
            this.messagesBanDuration = messagesBanDuration;
            this.banDuration = Duration.ofDays(messagesBanDuration);
        }
    }

    @Getter
    @Setter
    public static class Cache {
        private String prefix;
        private Integer ttl;
        private Integer verification;
    }

    @Getter
    @Setter
    public static class Security {
        private String superUserEmail;
        private String superUserPassword;
        private String oauthRedirectUrl;
        private String allowedOrigins;
        private String allowedMethods;
        private String allowedHeaders;
        private Boolean allowCredentials;
        private String decodeSignature;
        private String accessSecret;
        private String refreshSecret;
        private String accessLifetime;
        private String refreshLifetime;
        private Integer accessMaxAgeCookie;
        private Integer refreshMaxAgeCookie;
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

    @Getter
    @Setter
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String phoneNumber;
        private boolean enabled;
    }
}