package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.AuthResponse;
import com.bsu.cvbuilder.domain.TokenType;
import com.bsu.cvbuilder.entity.user.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface SecurityService {

    UserProfile findCurrentUser();

    AuthResponse authenticate(Authentication authentication);

    void logout();

    void entryPoint(HttpServletRequest request);

    void emailVerification();

    void checkOtp(String otp);

    String createOtp(String key);

    void checkToken(String token, TokenType tokenType);

    void checkSecureData();
}