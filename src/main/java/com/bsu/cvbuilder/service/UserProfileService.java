package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.user.UserProfile;

public interface UserProfileService {

    UserProfile findByEmail(String email);

    UserProfile findByLogin(String login);

    UserProfile findById(String id);

    Boolean existsByEmail(String email);

    UserProfile create(UserProfile build);

    UserProfile login(String login);

    UserProfile update(UserProfile profile);
}
