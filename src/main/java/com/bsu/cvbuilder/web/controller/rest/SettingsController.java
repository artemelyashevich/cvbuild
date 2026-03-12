package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.annotation.email.EmailVerification;
import com.bsu.cvbuilder.annotation.otp.OtpVerification;
import com.bsu.cvbuilder.domain.dto.auth.PasswordDto;
import com.bsu.cvbuilder.domain.dto.auth.ResetPasswordDto;
import com.bsu.cvbuilder.domain.dto.settings.UserSettings;
import com.bsu.cvbuilder.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public UserSettings findSettings() {
        return settingsService.findSettings();
    }

    @EmailVerification
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.CREATED)
    //@OtpVerification(key = "password:store:%s:", template = "store-password")
    public void setPassword(@RequestBody PasswordDto password) {
        settingsService.setPassword(password);
    }

    @EmailVerification
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.CREATED)
    //@OtpVerification(key = "password:restore:%s:", template = "restore-password")
    public void resetPassword(@RequestBody ResetPasswordDto resetPasswordDto) {
        settingsService.resetPassword(resetPasswordDto);
    }

    @EmailVerification
    @PostMapping("/agree")
    //@OtpVerification(key = "agreement:agree:%s:", template = "agree")
    public void agree() {
        settingsService.agree();
    }

    @EmailVerification
    @PostMapping("/2fa")
    public boolean enable2fa() {
        return settingsService.enable2fa();
    }

    @DeleteMapping
    @EmailVerification
    //@OtpVerification(key = "delete:account:%s:", template = "delete")
    public void removeAll(@RequestParam(required = false) String otp) {
        settingsService.deleteAccount(otp);
    }
}
