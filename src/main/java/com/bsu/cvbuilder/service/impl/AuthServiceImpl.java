package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.RefreshRequest;
import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import com.bsu.cvbuilder.domain.entity.security.SecureData;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.domain.event.UserLoginEvent;
import com.bsu.cvbuilder.service.AuthService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.bsu.cvbuilder.util.OAuthUtil.getOAuth2AuthenticationToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SecurityService securityService;
    private final UserProfileService userProfileService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.debug("Attempting authenticate user with email: {}", authRequest.email());

        UserProfile user = userProfileService.findByEmail(authRequest.email());

        securityService.checkSecureData(user, authRequest);

        var ctx = SecurityContextHolder.createEmptyContext();
        OAuth2AuthenticationToken authentication = getOAuth2AuthenticationToken(authRequest.email());
        ctx.setAuthentication(authentication);
        SecurityContextHolder.setContext(ctx);

        AuthResponse authResponse = securityService.authenticate(SecurityContextHolder.getContext().getAuthentication());

        applicationEventPublisher.publishEvent(UserLoginEvent.builder()
                        .userProfile(user)
                        .userId(user.getId())
                .build());
        log.debug("Authenticated user: {}", authRequest.email());
        return authResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterAuthDto authRequest) {
        log.debug("Attempting register user with email: {}", authRequest.email());
        UserProfile userProfile = userProfileService.create(UserProfile.builder()
                .firstName(authRequest.firstName())
                .lastName(authRequest.lastName())
                .login(authRequest.email())
                .email(authRequest.email())
                .build());
        securityService.loadSecureData(SecureData.builder()
                .password(authRequest.password())
                .userId(userProfile.getId())
                .build());
        return authenticate(new AuthRequest(authRequest.email(), authRequest.password()));
    }

    @Override
    public AuthResponse refreshToken(RefreshRequest refreshRequest) {
        return securityService.refreshAccessToken(refreshRequest.refreshToken());
    }
}
