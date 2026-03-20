package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.*;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.AbstractEvent;
import com.bsu.cvbuilder.domain.event.LogoutEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.security.SecureDataCacheSingleton;
import com.bsu.cvbuilder.service.*;
import com.bsu.cvbuilder.service.mapper.UserMapper;
import com.bsu.cvbuilder.util.OtpKeyUtil;
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
    private final JwtService jwtService;
    private final BlackListService blackListService;
    private final SecureDataService secureDataService;
    private final ApplicationProperties applicationProperties;
    private final OtpService otpService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationService notificationService;
    private final SecureDataCacheSingleton secureDataCacheSingleton;

    @Override
    @Monitored(value = "calling_auth_authenticate", context = "api")
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.debug("Attempting authenticate user with email: {}", authRequest.email());

        UserProfile user = userProfileService.findByEmail(authRequest.email());

        if (secureDataService.checkCredsAndIf2faIsRequire(user, authRequest)) {
            String otp = otpService.create(user, OtpKeyUtil.SECOND_AUTH_PHASE_KEY + user.getLogin());
            notificationService.sendNotification(NotificationDto.builder()
                            .engine(NotificationEngine.EMAIL)
                            .receiver(user.getEmail())
                            .parameters(Map.of("otp", otp))
                            .templateName("2fa")
                    .build());
            return AuthResponse.builder()
                    .secondPhaseEnabled(true)
                    .build();
        }

        log.debug("Authenticated user: {}", authRequest.email());
        return auth(user);
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
    public void registerWithRole(RegisterAuthDto authRequest, UserProfile.Role role) {
        log.debug("Attempting register user with role: {} email: {}", role, authRequest.email());
        UserProfile candidate = userMapper.toUserProfile(authRequest);
        candidate.setLogin(authRequest.email());
        candidate.setRole(role);
        UserProfile userProfile = userProfileService.create(candidate);
        secureDataService.loadSecureData(SecureData.builder()
                .password(authRequest.password())
                .userId(userProfile.getId())
                .build());
        auth(userProfile);
        SecurityContextHolder.clearContext();
    }

    @Override
    public AuthResponse refreshToken(RefreshRequest refreshRequest) {
        String refreshToken = refreshRequest.refreshToken();
        jwtService.validateToken(refreshToken, TokenType.REFRESH);
        String login = jwtService.extractLogin(refreshToken, TokenType.REFRESH);

        UserProfile user = userProfileService.findByLogin(login);
        SecureData secureData = secureDataService.findByUserId(user.getId());

        String currentEncoded = SecretDecodeUtil.encode(refreshToken, applicationProperties.getSecurity().getDecodeSignature());
        if (!currentEncoded.equals(secureData.getRefreshTokenEncoded())) {
            throw new AppException("Refresh token mismatch or revoked", 401);
        }

        String newAccessToken = jwtService.generateToken(user, TokenType.ACCESS);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void logout() {
        log.debug("Logout attempt");
        UserProfile userProfile = securityService.findCurrentUser();
        String token = securityService.getToken();
        Date expiration = jwtService.extractExpiration(token);
        blackListService.banToken(token, expiration);
        SecurityContextHolder.clearContext();
        publishLogoutEvent(token);
        secureDataCacheSingleton.clear(userProfile.getId());
        log.info("User logged out");
    }

    @Override
    public AuthResponse verify2fa(Verify2faRequest verify2faRequest) {
        log.debug("Attempting verify2fa user with email: {}", verify2faRequest.email());
        UserProfile user = userProfileService.findByEmail(verify2faRequest.email());
        if (!otpService.validateOtp(user, verify2faRequest.code(), OtpKeyUtil.SECOND_AUTH_PHASE_KEY + user.getLogin())) {
            throw new AppException("Некорректный одноразовый пароль", 401);
        };
        log.info("Verify2fa user with email: {}", verify2faRequest.email());
        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.EMAIL)
                .receiver(user.getEmail())
                .parameters(Map.of())
                .templateName("2fa_success")
                .build());
        return auth(user);
    }

    @Override
    public void verify2faRefresh(Verify2faRefreshRequest verify2faRefreshRequest) {
        log.debug("Attempting refresh verify2fa code user with email: {}", verify2faRefreshRequest.email());
        UserProfile user = userProfileService.findByEmail(verify2faRefreshRequest.email());
        otpService.invalidate(OtpKeyUtil.SECOND_AUTH_PHASE_KEY + user.getLogin());
        String otp = otpService.create(user, OtpKeyUtil.SECOND_AUTH_PHASE_KEY + user.getLogin());
        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.EMAIL)
                .receiver(user.getEmail())
                .parameters(Map.of("otp", otp))
                .templateName("2fa")
                .build());
        log.info("Verify2fa user with email refresh code: {}", verify2faRefreshRequest.email());
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

    private AuthResponse auth(UserProfile userProfile) {
        var ctx = SecurityContextHolder.createEmptyContext();
        OAuth2AuthenticationToken authentication = getOAuth2AuthenticationToken(userProfile.getLogin(), userProfile.getRole(), null);
        ctx.setAuthentication(authentication);
        SecurityContextHolder.setContext(ctx);
        return securityService.authenticate(SecurityContextHolder.getContext().getAuthentication());
    }
}
