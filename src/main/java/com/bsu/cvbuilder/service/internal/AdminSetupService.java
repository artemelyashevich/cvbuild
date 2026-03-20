package com.bsu.cvbuilder.service.internal;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.service.AuthService;
import com.bsu.cvbuilder.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSetupService implements ApplicationRunner {

    private final AuthService authService;
    private final UserProfileService userProfileService;
    private final ApplicationProperties applicationProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userProfileService.existsByEmail(applicationProperties.getSecurity().getSuperUserEmail())) {
            return;
        }
        authService.registerWithRole(RegisterAuthDto.builder()
                .email(applicationProperties.getSecurity().getSuperUserEmail())
                .password(applicationProperties.getSecurity().getSuperUserPassword())
                .firstName("Super")
                .lastName("User")
                .build(), UserProfile.Role.SUPER_ADMIN);
    }
}
