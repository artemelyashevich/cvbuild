package com.bsu.cvbuilder.domain.entity.security;

import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Document(collection = "secureData")
public class SecureData {

    private String id;

    private String userId;

    @Builder.Default
    private NotificationEngine preferableNotificationEngine = NotificationEngine.WS;

    @Builder.Default
    private Set<NotificationEngine> notificationEngines = Set.of(NotificationEngine.WS);

    @ToString.Exclude
    private String refreshTokenEncoded;

    @ToString.Exclude
    private SecureInfo data;

    @ToString.Exclude
    private String password;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SecureInfo {
        private String passwordSalt;
        private String passwordHash;
        private String ipAddress;
        private String country;
    }
}
