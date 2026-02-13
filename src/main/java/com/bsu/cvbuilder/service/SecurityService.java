package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.EmailVerificationRequestDto;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import org.springframework.security.core.Authentication;

public interface SecurityService {

    UserProfile findCurrentUser();

    AuthResponse authenticate(Authentication authentication);

    void checkOtp(String otp);

    void verifyEmailRequest(EmailVerificationRequestDto emailVerificationRequestDto);
}