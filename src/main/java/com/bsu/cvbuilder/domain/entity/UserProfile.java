package com.bsu.cvbuilder.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
@ToString(onlyExplicitlyIncluded = true)
@Document(collection = "users")
@CompoundIndex(name = "verification_idx", def = "{'emailVerified': 1, 'createdAt': 1}")
public class UserProfile {

    @Id
    @ToString.Include
    private String id;

    private String email;

    @ToString.Include
    private String login;

    private String firstName;

    private String lastName;

    @ToString.Include
    private String avatarUrl;

    @Builder.Default
    @ToString.Include
    private Role role = Role.USER;

    @Builder.Default
    @Indexed(name = "email_verified_idx")
    private Boolean emailVerified = false;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @CreatedDate
    private LocalDateTime createdAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Indexed(name = "last_login_idx", direction = IndexDirection.DESCENDING)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLogin;

    @Builder.Default
    @ToString.Include
    private boolean isAgree = false;

    @Builder.Default
    private List<AiLimit> aiLimits = new ArrayList<>();

    @Builder.Default
    private Locale locale = Locale.ENGLISH;

    public enum Role {
        USER
    }
}
