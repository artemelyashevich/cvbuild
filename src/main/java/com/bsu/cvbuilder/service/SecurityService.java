package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.user.UserProfile;
import org.springframework.security.core.Authentication;

public interface SecurityService {

    UserProfile findCurrentUser();

    void authenticate(Authentication authentication);

    void logout();

    void emailVerification();

    void checkOtp(String otp);

    String createOtp(String key);
}