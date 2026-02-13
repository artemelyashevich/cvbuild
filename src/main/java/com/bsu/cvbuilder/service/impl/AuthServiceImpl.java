package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.RefreshRequest;
import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import com.bsu.cvbuilder.domain.dto.auth.SecurityProvider;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.AbstractEvent;
import com.bsu.cvbuilder.domain.event.LogoutEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AuthService;
import com.bsu.cvbuilder.service.BlackListService;
import com.bsu.cvbuilder.service.JwtService;
import com.bsu.cvbuilder.service.SecureDataService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.service.mapper.UserMapper;
import com.bsu.cvbuilder.util.SecretDecodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

import static com.bsu.cvbuilder.util.OAuthUtil.getOAuth2AuthenticationToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final SecurityService securityService;
    private final UserProfileService userProfileService;
    private final SecurityProvider securityProvider;
    private final JwtService jwtService;
    private final BlackListService blackListService;
    private final SecureDataService secureDataService;
    private final ApplicationProperties applicationProperties;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Monitored(value = "calling_auth_authenticate", context = "api")
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.debug("Attempting authenticate user with email: {}", authRequest.email());

        UserProfile user = userProfileService.findByEmail(authRequest.email());

        secureDataService.checkData(user, authRequest);

        var ctx = SecurityContextHolder.createEmptyContext();
        OAuth2AuthenticationToken authentication = getOAuth2AuthenticationToken(authRequest.email(), user.getRole());
        ctx.setAuthentication(authentication);
        SecurityContextHolder.setContext(ctx);

        AuthResponse authResponse = securityService.authenticate(SecurityContextHolder.getContext().getAuthentication());

        log.debug("Authenticated user: {}", authRequest.email());
        return authResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Monitored(value = "calling_auth_register", context = "api")
    public AuthResponse register(RegisterAuthDto authRequest) {
        log.debug("Attempting register user with email: {}", authRequest.email());
        UserProfile candidate = userMapper.toUserProfile(authRequest);
        candidate.setLogin(authRequest.email());
        UserProfile userProfile = userProfileService.create(candidate);
        secureDataService.loadSecureData(SecureData.builder()
                .password(authRequest.password())
                .userId(userProfile.getId())
                .build());
        return authenticate(new AuthRequest(authRequest.email(), authRequest.password()));
    }

    @Override
    public AuthResponse refreshToken(RefreshRequest refreshRequest) {
        String refreshToken = refreshRequest.refreshToken();
        jwtService.validateToken(refreshToken, TokenType.REFRESH);
        String login = jwtService.extractLogin(refreshToken, TokenType.REFRESH);

        UserProfile user = userProfileService.findByEmail(login);
        SecureData secureData = secureDataService.findByUserId(user.getId());

        String currentEncoded = SecretDecodeUtil.encode(refreshToken, applicationProperties.getSecurity().getDecodeSignature());
        if (!currentEncoded.equals(secureData.getRefreshTokenEncoded())) {
            throw new AppException("Refresh token mismatch or revoked", 401);
        }

        String newAccessToken = jwtService.generateToken(user, TokenType.ACCESS);

        return new AuthResponse(newAccessToken, refreshToken);
    }

    @Override
    public void logout() {
        String token = securityProvider.getToken();
        Date expiration = jwtService.extractExpiration(token);
        blackListService.banToken(token, expiration);
        SecurityContextHolder.clearContext();
        publishLogoutEvent(token);
    }

    private void publishLogoutEvent(String token) {
        try {
            String login = jwtService.extractLogin(token, TokenType.ACCESS);
            AbstractEvent logoutEvent = LogoutEvent.builder()
                    .userId(null)
                    .build();
            logoutEvent.setData(Map.of("login", login));
            applicationEventPublisher.publishEvent(logoutEvent);
        } catch (Exception e) {
            log.warn("Could not publish logout event: user profile not found");
        }
    }
}
