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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
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
import java.util.Map;

import static com.bsu.cvbuilder.util.OAuthUtil.getOAuth2AuthenticationToken;

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
    private final ApplicationContext applicationContext;

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

        var secureData = secureDataRepository.findByUserId(user.getId()).orElse(
                SecureData.builder()
                        .userId(user.getId())
                        .refreshTokenEncoded(SecretDecodeUtil.encode(
                                jwtService.generateToken(user, TokenType.REFRESH),
                                applicationProperties.getSecurity().getDecodeSignature()))
                        .build()
        );

        var refreshToken = jwtService.generateToken(user, TokenType.REFRESH);

        if (secureData.getRefreshTokenEncoded() == null) {


            var encodedToken = SecretDecodeUtil.encode(
                    refreshToken,
                    applicationProperties.getSecurity().getDecodeSignature()
            );

            secureData.setRefreshTokenEncoded(encodedToken);
        } else {
            try {
                String decryptedToken = SecretDecodeUtil.decode(
                        secureData.getRefreshTokenEncoded(),
                        applicationProperties.getSecurity().getDecodeSignature()
                );
                checkToken(decryptedToken, TokenType.REFRESH);
            } catch (AppException e) {
                secureData.setRefreshTokenEncoded(null);
                secureDataRepository.save(secureData);
                throw e;
            }
        }

        secureDataRepository.save(secureData);

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

        return new AuthResponse(jwtService.generateToken(user, TokenType.ACCESS), refreshToken);
    }

    @Override
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
    public void verifyEmail(String otp) {
        checkOtp(otp);
    }

    @Override
    public void checkToken(String token, TokenType tokenType) {
        jwtService.validateToken(token, tokenType);
    }

    @Override
    public void checkSecureData(UserProfile userProfile, AuthRequest authRequest) {
        SecureData secureData = secureDataRepository.findByUserId(userProfile.getId()).orElseThrow(
                () -> new AppException("Invalid user profile", 401)
        );
        if (!passwordEncoder.matches(authRequest.password(), secureData.getPassword())) {
            log.debug("Password does not match stored value, email: {}", authRequest.email());
            throw new AppException("Password mismatch", 401);
        }
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
    public AuthResponse refreshAccessToken(String refreshToken) {
        jwtService.validateToken(refreshToken, TokenType.REFRESH);
        String login = jwtService.extractLogin(refreshToken, TokenType.REFRESH);
        String id = userProfileService.findByEmail(login).getId();
        var secureData = secureDataRepository.findByUserId(id).orElseThrow(
                () -> new AppException("Invalid user profile", 401)
        );
        if (!secureData.getRefreshTokenEncoded().equals(
                SecretDecodeUtil.encode(refreshToken, applicationProperties.getSecurity().getDecodeSignature())
        )) {
            throw new AppException("Invalid refresh token", 401);
        }
        var ctx = SecurityContextHolder.createEmptyContext();
        OAuth2AuthenticationToken authentication = getOAuth2AuthenticationToken(login);
        ctx.setAuthentication(authentication);
        SecurityContextHolder.setContext(ctx);
        return applicationContext.getBean(SecurityService.class).authenticate(authentication);
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
