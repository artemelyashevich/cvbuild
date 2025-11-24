package com.bsu.cvbuilder.entity.user;

import com.bsu.cvbuilder.entity.limit.AiLimit;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Document(collection = "users")
@CompoundIndexes({
        @CompoundIndex(name = "email_subscription_idx", def = "{'email': 1, 'subscriptionTier': 1}"),
        @CompoundIndex(name = "verification_idx", def = "{'emailVerified': 1, 'createdAt': 1}")
})
public class UserProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String firstName;

    private String lastName;

    @ToString.Exclude
    private String password;

    private String avatarUrl;

    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    @Indexed(name = "email_verified_idx")
    private Boolean emailVerified =false;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @CreatedDate
    private LocalDateTime createdAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Indexed(name = "last_login_idx", direction = IndexDirection.DESCENDING)
    private LocalDateTime lastLogin;

    @ToString.Exclude
    private UserPreferences userPreferences;

    @ToString.Exclude
    private UserStats userStats;

    @Builder.Default
    private List<AiLimit> aiLimits = new ArrayList<>();

    @Builder.Default
    private Locale locale = Locale.ENGLISH;

    public enum Role {
        USER
    }
}
