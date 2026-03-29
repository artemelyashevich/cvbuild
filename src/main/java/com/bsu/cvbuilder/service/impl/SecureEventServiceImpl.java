package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.PasswordDto;
import com.bsu.cvbuilder.domain.dto.auth.ResetPasswordDto;
import com.bsu.cvbuilder.domain.entity.SecureEvent;
import com.bsu.cvbuilder.service.SecureEventService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecureEventServiceImpl implements SecureEventService {

    private final SettingsService settingsService;

    @Override
    public void handleEvent(SecureEvent event, Object dto) {
        log.debug("[SECURE-OTP] Attempting handle new event: {}", event);
        switch (event) {
            case verifyEmail -> settingsService.setVerification();
            case resetPassword -> settingsService.resetPassword((ResetPasswordDto) dto);
            case setPassword -> settingsService.setPassword((PasswordDto) dto);
            case enable2fa -> settingsService.enable2fa();
            default -> throw new IllegalStateException("Unexpected value: " + event);
        }
    }
}
