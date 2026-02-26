package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.annotation.email.EmailVerification;
import com.bsu.cvbuilder.domain.dto.auth.PasswordDto;
import com.bsu.cvbuilder.domain.dto.auth.ResetPasswordDto;
import com.bsu.cvbuilder.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @EmailVerification
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.CREATED)
    public void setPassword(@RequestBody PasswordDto password) {
        settingsService.setPassword(password);
    }

    @EmailVerification
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.CREATED)
    public void resetPassword(@RequestBody ResetPasswordDto resetPasswordDto) {
        settingsService.resetPassword(resetPasswordDto);
    }

    @EmailVerification
    @PostMapping("/agree")
    public void agree() {
        settingsService.agree();
    }

    @EmailVerification
    @PostMapping("/2fa")
    public boolean disagree() {
        return settingsService.enable2fa();
    }

    @EmailVerification
    @DeleteMapping
    public void removeAll() {
        settingsService.deleteAccount();
    }
}
