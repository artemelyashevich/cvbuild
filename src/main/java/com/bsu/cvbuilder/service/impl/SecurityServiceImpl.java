package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.dto.EmailDto;
import com.bsu.cvbuilder.entity.user.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.EmailService;
import com.bsu.cvbuilder.service.RedisService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Value("${app.security.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Override
    public UserProfile findCurrentUser() {
        log.debug("Attempting to get current user profile");

        var login = extractEmail(SecurityContextHolder.getContext().getAuthentication());

        var user = currentUser.get();

        if (user == null) {
            user = userProfileService.findByEmail(login);
            currentUser.set(user);
        }

        log.info("Current user profile found: {}", user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authenticate(Authentication authentication) {
        log.debug("Attempting to authenticate User via OAuth2");

        var email = extractEmail(SecurityContextHolder.getContext().getAuthentication());

        var user = userProfileService.login(email);

        if (user == null) {
            var message = String.format("Invalid email or password: %s", email);
            log.error(message);
            throw new AppException(message, 500);
        }

        currentUser.set(user);
    }

    @Override
    public void logout() {
        currentUser.remove();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    public void entryPoint(HttpServletRequest request) {

    }

    @Override
    public void emailVerification() {
        var email = extractEmail(SecurityContextHolder.getContext().getAuthentication());
        var otp = redisService.getOtp(email);
        new Thread(
                () -> emailService.sendEmail(
                        EmailDto.builder()
                                .sender("")
                                .receiver(email)
                                .activationCode(otp)
                                .template("")
                                .build()
                )
        ).start();
    }

    @Override
    public void checkOtp(String otp) {
        var email = extractEmail(SecurityContextHolder.getContext().getAuthentication());
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
    public boolean isTokenValid(String token) {
        return false;
    }

    @Override
    public boolean isTokenExpire(String authToken) {
        return false;
    }

    @Override
    public void checkSecureData() {

    }

    private String extractEmail(Authentication authentication) {
        var authToken = validateAuthentication(authentication);
        var principal = extractPrincipal(authToken);
        return principal.getAttribute("login");
    }

    private AbstractAuthenticationToken validateAuthentication(Authentication authentication) {
        if (oauth2Enabled) {
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
