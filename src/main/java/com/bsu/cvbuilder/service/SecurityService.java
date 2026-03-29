package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.auth.AuthResponse;
import com.bsu.cvbuilder.domain.dto.auth.EmailVerificationRequestDto;
import com.bsu.cvbuilder.domain.entity.SecureEvent;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.web.dto.otp.OtpRequest;
import org.springframework.security.core.Authentication;

public interface SecurityService {

    UserProfile findCurrentUser();

    AuthResponse authenticate(Authentication authentication);

    void checkOtp(OtpRequest otp, SecureEvent verifyEmail);

    void verifyEmailRequest(EmailVerificationRequestDto emailVerificationRequestDto);

    String getToken();
}