package com.bsu.cvbuilder.domain.entity;

import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

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

    @Builder.Default
    private Boolean secondAuthPhaseRequire = false;

    @Builder.Default
    @Indexed(name = "email_verified_idx")
    private Boolean emailVerified = false;

    @Builder.Default
    @ToString.Include
    private boolean isAgree = false;

    @Builder.Default
    @ToString.Include
    private boolean secondAuthPhase = false;

    @ToString.Exclude
    @Builder.Default
    private Map<SecureEvent, List<LocalDateTime>> secureEvents = new EnumMap<>(SecureEvent.class);

    @Version
    private Long version;

    public void addEvent(SecureEvent event) {
        if (this.secureEvents == null) {
            this.secureEvents = new EnumMap<>(SecureEvent.class);
        }
        this.secureEvents.computeIfAbsent(event, k -> new ArrayList<>())
                .add(LocalDateTime.now());
    }

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
