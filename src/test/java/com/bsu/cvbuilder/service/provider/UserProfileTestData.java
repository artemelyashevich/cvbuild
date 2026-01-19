package com.bsu.cvbuilder.service.provider;

import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import java.time.LocalDateTime;
import java.util.UUID;

public final class UserProfileTestData {

    private UserProfileTestData() {}

    public static UserProfile createNewProfile(String email, String login) {
        return UserProfile.builder()
                .email(email)
                .login(login)
                .firstName("John")
                .lastName("Doe")
                .lastLogin(LocalDateTime.now())
                .build();
    }

    public static UserProfile createExistingProfile(String id, String email, String login) {
        var profile = createNewProfile(email, login);
        profile.setId(id);
        return profile;
    }
}