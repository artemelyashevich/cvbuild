package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.user.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface SecurityService {

    UserProfile findCurrentUser();

    void authenticate(Authentication authentication);

    void logout();

    void entryPoint(HttpServletRequest request);

    void emailVerification();

    void checkOtp(String otp);

    String createOtp(String key);

    boolean isTokenValid(String token);

    boolean isTokenExpire(String authToken);

    void checkSecureData();
}