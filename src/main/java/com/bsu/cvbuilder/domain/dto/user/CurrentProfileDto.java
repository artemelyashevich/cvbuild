package com.bsu.cvbuilder.domain.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentProfileDto {
    private String id;

    private String email;

    private String login;

    private String firstName;

    private String lastName;

    private String avatarUrl;

    private Boolean emailVerified;

    private Boolean secondAuthPhaseEnabled;

    private LocalDateTime lastLogin;

    private LocalDateTime createdAt;
}
