package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.*;
import com.bsu.cvbuilder.domain.entity.security.SecureData;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.SecureDataRepository;
import com.bsu.cvbuilder.service.*;
import com.bsu.cvbuilder.util.SecretDecodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    @Lazy
    private final UserProfileService userProfileService;
    private final RedisService redisService;
    private final JwtService jwtService;
    private final SecureDataRepository secureDataRepository;
    private final ApplicationProperties applicationProperties;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final SecureDataService secureDataService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.security.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Override
    public UserProfile findCurrentUser() {
        log.debug("Attempting to get current user profile");

        var login = extractLogin(SecurityContextHolder.getContext().getAuthentication());

        var user = userProfileService.findByLogin(login);

        log.info("Current user profile found: {}", user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse authenticate(Authentication authentication) {
        log.debug("Attempting to authenticate User");

        var login = extractLogin(authentication);

        var user = userProfileService.login(login);

        if (user == null) {
            var message = String.format("Invalid login or password: %s", login);
            log.error(message);
            throw new AppException(message, 500);
        }

        var secureData = secureDataService.prepareData(user);

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            notificationService.sendNotification(
                    NotificationDto.builder()
                            .parameters(Map.of("message", "Please, provide and verify your email"))
                            .engine(NotificationEngine.WS)
                            .receiver(user.getLogin())
                            .templateName("")
                            .build()
            );
        }

        return new AuthResponse(
                jwtService.generateToken(user, TokenType.ACCESS),
                SecretDecodeUtil.decode(secureData.getRefreshTokenEncoded(), applicationProperties.getSecurity().getDecodeSignature())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkOtp(String otp) {
        UserProfile profile = findCurrentUser();
        var otpFromCache = redisService.getOtp(buildOtpKey(profile.getEmail()));
        if (!otpFromCache.equals(otp)) {
            throw new AppException("Otp mismatch", 401);
        }
        profile.setEmailVerified(true);
        userProfileService.update(profile);
        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.EMAIL)
                .receiver(profile.getEmail())
                .templateName("email_verification_success")
                .parameters(Map.of(
                        "login", profile.getLogin()
                ))
                .build());
    }

    @Override
    public void verifyEmailRequest() {
        UserProfile userProfile = findCurrentUser();
        if (userProfile.getEmail() == null || userProfile.getEmail().isEmpty()) {
            throw new AppException("Invalid email", 401);
        }
        if (userProfile.getEmailVerified()) {
            return;
        }
        String otp = String.format("%06d", new SecureRandom().nextInt(1000000));
        redisService.putOtp(buildOtpKey(userProfile.getEmail()), otp);
        notificationService.sendNotification(NotificationDto.builder()
                .engine(NotificationEngine.EMAIL)
                .receiver(userProfile.getEmail())
                .templateName("email_verification")
                .parameters(Map.of(
                        "otp", otp
                ))
                .build());
    }

    @Override
    public void checkToken(String token, TokenType tokenType) {
        Boolean isBlacklisted = redisTemplate.hasKey(token);
        if (isBlacklisted) {
            throw new AppException("This token is banned", 401);
        }
        jwtService.validateToken(token, tokenType);
    }

    @Override
    public void checkSecureData(UserProfile userProfile, AuthRequest authRequest) {
        secureDataService.checkData(userProfile, authRequest);
    }

    @Override
    public String extractSubject(String token) {
        return jwtService.extractLogin(token, TokenType.ACCESS);
    }

    @Override
    public void loadSecureData(SecureData secureData) {
        SecureData persistentData = SecureData.builder()
                .password(passwordEncoder.encode(secureData.getPassword()))
                .userId(secureData.getUserId())
                .build();
        secureDataRepository.save(persistentData);
    }

    @Override
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        jwtService.validateToken(refreshToken, TokenType.REFRESH);
        String login = jwtService.extractLogin(refreshToken, TokenType.REFRESH);

        UserProfile user = userProfileService.findByEmail(login);
        SecureData secureData = secureDataRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException("Security data not found", 401));

        String currentEncoded = SecretDecodeUtil.encode(refreshToken, applicationProperties.getSecurity().getDecodeSignature());
        if (!currentEncoded.equals(secureData.getRefreshTokenEncoded())) {
            throw new AppException("Refresh token mismatch or revoked", 401);
        }

        String newAccessToken = jwtService.generateToken(user, TokenType.ACCESS);

        return new AuthResponse(newAccessToken, refreshToken);
    }

    @Override
    public Date getJwtExpiration(String token) {
        return jwtService.extractExpiration(token);
    }

    private String extractLogin(Authentication authentication) {
        var authToken = validateAuthentication(authentication);
        var principal = extractPrincipal(authToken);
        return principal.getAttribute("login");
    }

    private AbstractAuthenticationToken validateAuthentication(Authentication authentication) {
        if (oauth2Enabled) {
            if (authentication instanceof UsernamePasswordAuthenticationToken authToken) {
                return authToken;
            }
            if (!(authentication instanceof OAuth2AuthenticationToken authToken)) {
                log.debug("Invalid authentication type: {}", authentication.getClass());
                throw new AppException("Unsupported authentication type", 401);
            }
            return authToken;
        }
        if (!(authentication instanceof UsernamePasswordAuthenticationToken authToken)) {
            log.debug("Invalid authentication type: {}", authentication.getClass());
            throw new AppException("Unsupported authentication type", 401);
        }
        return authToken;
    }

    private String buildOtpKey(String email) {
        return String.format("::%s", email);
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
