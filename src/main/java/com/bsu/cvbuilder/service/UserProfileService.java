package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserProfile findByEmail(String email);

    UserProfile findByLogin(String login);

    UserProfile findById(String id);

    Boolean existsByEmail(String email);

    UserProfile create(UserProfile build);

    UserProfile login(String login);

    UserProfile update(UserProfile profile);

    UserProfile uploadAvatar(MultipartFile file, String id);

    void updateEmail(String id, String email);

    void deleteById(String id);
}
