package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.*;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.SecureEvent;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.AbstractEvent;
import com.bsu.cvbuilder.domain.event.LoginEvent;
import com.bsu.cvbuilder.domain.event.VerifyEmailRequestEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.JwtService;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.OtpService;
import com.bsu.cvbuilder.service.SecureDataService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.util.CacheUtil;
import com.bsu.cvbuilder.util.SecretDecodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private final UserProfileService userProfileService;
    private final JwtService jwtService;
    private final ApplicationProperties applicationProperties;
    private final NotificationService notificationService;
    private final SecureDataService secureDataService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OtpService otpService;

    @Value("${app.security.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Override
    @Monitored(value = "security_service.current", context = "internal")
    public UserProfile findCurrentUser() {
        var login = getCurrentUserLogin();
        return userProfileService.findByLogin(login);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Monitored(value = "security_service.authentication", context = "internal")
    public AuthResponse authenticate(Authentication authentication) {
        var login = extractLogin(authentication);

        log.debug("Attempting to authenticate User: {}", login);

        var user = userProfileService.login(login);

        if (user == null) {
            var message = String.format("Invalid login or password: %s", login);
            log.error(message);
            throw new AppException(message, 401);
        }

        Map<String, String> data = new HashMap<>();

        AbstractEvent event = LoginEvent.builder()
                .userId(user.getId())
                .userProfile(user)
                .build();

        AuthResponse response = null;

        try {
            var secureData = secureDataService.prepareData(user);

            response = AuthResponse.builder()
                    .accessToken(jwtService.generateToken(user, TokenType.ACCESS))
                    .refreshToken(SecretDecodeUtil.decode(secureData.getRefreshTokenEncoded(), applicationProperties.getSecurity().getDecodeSignature()))
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage());
            data.put("error", e.getMessage());
        } finally {
            event.setData(data);
            applicationEventPublisher.publishEvent(event);
        }
        response.setRole(user.getRole().name());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkOtp(String otp, SecureEvent secureEvent) {
        UserProfile profile = findCurrentUser();
        if (!otpService.validateOtp(profile, otp, CacheUtil.EMAIL_KEY + profile.getEmail())) {
            throw new AppException("Invalid OTP", 401);
        }
        secureDataService.performEvent(profile, secureEvent);
        sendVerificationSuccessNotification(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyEmailRequest(EmailVerificationRequestDto emailVerificationRequestDto) {
        UserProfile userProfile = findCurrentUser();
        SecureData secureData = secureDataService.findByUserId(userProfile.getId());
        AbstractEvent event = new VerifyEmailRequestEvent(userProfile.getId());
        Map<String, String> data = new HashMap<>();

        if (secureData.getEmailVerified()) { // NOSONAR
            data.put("status", "alreadyVerified");
            event.setData(data);
            applicationEventPublisher.publishEvent(event);
            return;
        }
        secureDataService.validateNewEvent(userProfile.getId(), SecureEvent.verifyEmail);
        String otp = otpService.create(userProfile, CacheUtil.EMAIL_KEY + userProfile.getEmail());
        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.EMAIL)
                .receiver(userProfile.getEmail())
                .templateName("email_verification")
                .parameters(Map.of(
                        "otp", otp
                ))
                .build());
        data.put("otp", "sent");
        event.setData(data);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public String getToken() {
        var authToken = validateAuthentication(SecurityContextHolder.getContext().getAuthentication());
        var principal = extractPrincipal(authToken);
        return principal.getAttribute("token");
    }

    private String getCurrentUserLogin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return extractLogin(auth);
    }

    private String extractLogin(Authentication authentication) {
        var authToken = validateAuthentication(authentication);
        var principal = extractPrincipal(authToken);
        return principal.getAttribute("login");
    }

    private AbstractAuthenticationToken validateAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new AppException("Invalid authentication", 401);
        }
        if (oauth2Enabled) {
            if (authentication instanceof UsernamePasswordAuthenticationToken authToken) {
                return authToken;
            }
            if (!(authentication instanceof OAuth2AuthenticationToken authToken)) {
                log.warn("Invalid authentication type: {}", authentication.getClass());
                throw new AppException("Unsupported authentication type", 401);
            }
            return authToken;
        }
        if (!(authentication instanceof UsernamePasswordAuthenticationToken authToken)) {
            log.warn("Invalid authentication type: {}", authentication.getClass());
            throw new AppException("Unsupported authentication type", 401);
        }
        return authToken;
    }

    private void sendVerificationSuccessNotification(UserProfile profile) {
        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.EMAIL)
                .receiver(profile.getEmail())
                .templateName("email_verification_success")
                .parameters(Map.of("login", profile.getLogin()))
                .build());
    }

    private DefaultOAuth2User extractPrincipal(AbstractAuthenticationToken authToken) {
        try {
            return (DefaultOAuth2User) authToken.getPrincipal();
        } catch (ClassCastException e) {
            log.debug("Invalid principal type", e);
            throw new AppException("Invalid user principal", 401);
        }
    }
}
