package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.AuthRequest;
import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.security.SecureData;
import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface SecurityService {

    UserProfile findCurrentUser();

    AuthResponse authenticate(Authentication authentication);

    void logout();

    void checkOtp(String otp);

    String createOtp(String key);

    void checkToken(String token, TokenType tokenType);

    void checkSecureData(UserProfile userProfile, AuthRequest authRequest);

    String extractSubject(String token);

    void loadSecureData(SecureData secureData);
}