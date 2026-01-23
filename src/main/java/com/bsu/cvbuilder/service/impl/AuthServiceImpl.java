package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.*;
import com.bsu.cvbuilder.domain.entity.security.SecureData;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AuthService;
import com.bsu.cvbuilder.service.JwtService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static com.bsu.cvbuilder.util.OAuthUtil.getOAuth2AuthenticationToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SecurityService securityService;
    private final UserProfileService userProfileService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityProvider securityProvider;
    private final JwtService jwtService;

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

        /*applicationEventPublisher.publishEvent(UserLoginEvent.builder()
                        .userProfile(user)
                        .userId(user.getId())
                .build());*/
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

    @Override
    public void logout() {
        String token = securityProvider.getToken();
        long expiration = securityService.getJwtExpiration(token).getTime();
        long now = System.currentTimeMillis();
        long duration = expiration - now;

        if (duration > 0) {
            redisTemplate.opsForValue().set(token, "revoked", Duration.ofMillis(duration));
        }
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    public AuthResponse resetPassword(ResetPasswordDto resetPasswordDto) {
        log.debug("Attempting reset password");

        if (!resetPasswordDto.newPassword().equals(resetPasswordDto.confirmedNewPassword())) {
            log.debug("Passwords do not match");
            throw new AppException("Password do not match", 400);
        }

        securityService.resetPassword(resetPasswordDto);

        return null;
    }
}
