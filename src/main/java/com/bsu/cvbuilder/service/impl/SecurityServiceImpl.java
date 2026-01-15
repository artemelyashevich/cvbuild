package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    @Lazy
    private final UserProfileService userProfileService;
    private final ThreadLocal<UserProfile> currentUser = new ThreadLocal<>();
    private final EmailService emailService;
    private final RedisService redisService;
    private final JwtService jwtService;
    private final SecureDataRepository secureDataRepository;
    private final ApplicationProperties applicationProperties;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Override
    public UserProfile findCurrentUser() {
        log.debug("Attempting to get current user profile");

        var login = extractLogin(SecurityContextHolder.getContext().getAuthentication());

        var user = currentUser.get();

        if (user == null) {
            user = userProfileService.findByLogin(login);
            currentUser.set(user);
        }

        log.info("Current user profile found: {}", user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse authenticate(Authentication authentication) {
        log.debug("Attempting to authenticate User via OAuth2");

        var email = extractLogin(SecurityContextHolder.getContext().getAuthentication());

        var user = userProfileService.login(email);

        if (user == null) {
            var message = String.format("Invalid email or password: %s", email);
            log.error(message);
            throw new AppException(message, 500);
        }

        currentUser.set(user);

        var secureData = secureDataRepository.findByUserId(user.getId()).orElse(
                SecureData.builder()
                        .userId(user.getId())
                        .refreshTokenEncoded(SecretDecodeUtil.encode(
                                jwtService.generateToken(user, TokenType.REFRESH),
                                applicationProperties.getSecurity().getDecodeSignature()))
                        .build()
        );

        if (secureData.getRefreshTokenEncoded() == null) {
            var refreshToken = jwtService.generateToken(user, TokenType.REFRESH);

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

        return new AuthResponse(jwtService.generateToken(user, TokenType.ACCESS), secureData.getRefreshTokenEncoded());
    }

    @Override
    public void logout() {
        currentUser.remove();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    public void checkOtp(String otp) {
        var email = extractLogin(SecurityContextHolder.getContext().getAuthentication());
        var otpFromCache = redisService.getOtp(buildOtpKey(email));
        if (!otpFromCache.equals(otp)) {
            throw new AppException("Otp mismatch", 401);
        }
    }

    @Override
    public String createOtp(String key) {
        var otp = String.format("%06d", new SecureRandom().nextInt(1000000));
        redisService.putOtp(key, otp);
        return otp;
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
                .password(secureData.getPassword())
                .userId(secureData.getUserId())
                .build();
        secureDataRepository.save(persistentData);
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
        return String.format("::%s::", email);
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
