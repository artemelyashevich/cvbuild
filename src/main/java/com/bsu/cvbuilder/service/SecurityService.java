package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.EmailVerificationRequestDto;
import com.bsu.cvbuilder.domain.entity.SecureData;
import com.bsu.cvbuilder.domain.entity.SecureEvent;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import org.springframework.security.core.Authentication;

import java.util.function.Consumer;

public interface SecurityService {

    UserProfile findCurrentUser();

    AuthResponse authenticate(Authentication authentication);

    void checkOtp(String otp, SecureEvent verifyEmail, Consumer<SecureData> updater);

    void verifyEmailRequest(EmailVerificationRequestDto emailVerificationRequestDto);

    String getToken();
}