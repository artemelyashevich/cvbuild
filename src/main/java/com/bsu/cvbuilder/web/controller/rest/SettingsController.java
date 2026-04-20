package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.annotation.email.EmailVerification;
import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import com.bsu.cvbuilder.domain.dto.auth.PasswordDto;
import com.bsu.cvbuilder.domain.dto.auth.ResetPasswordDto;
import com.bsu.cvbuilder.domain.dto.notification.WsType;
import com.bsu.cvbuilder.domain.dto.settings.UserSettings;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final NotificationService notificationService;

    @PatchMapping
    public void send(@RequestParam String message) {
        notificationService.sendNotification(
                NotificationDto.builder()
                        .receiver(SecurityContextHolder.getContext().getAuthentication().getName())
                        .engine(NotificationEngine.WS)
                        .parameters(Map.of("message", message, "type", WsType.SUCCESS))
                        .build()
        );
    }

    @GetMapping
    public UserSettings findSettings() {
        return settingsService.findSettings();
    }

    @PostMapping("/limits/{userId}")
    public void deactivateLimits(@PathVariable("userId") String userId) {
        settingsService.deactivateLimits(userId);
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
    @ResponseStatus(HttpStatus.ACCEPTED)
    //@OtpVerification(key = "agreement:agree:%s:", template = "agree")
    public boolean agree() {
        return settingsService.agree();
    }

    @EmailVerification
    @PostMapping("/2fa")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public boolean enable2fa() {
        return settingsService.enable2fa();
    }

    @DeleteMapping
    @EmailVerification
    @ResponseStatus(HttpStatus.NO_CONTENT)
    //@OtpVerification(key = "delete:account:%s:", template = "delete")
    public void removeAll(@RequestParam(required = false) String otp) {
        settingsService.deleteAccount(otp);
    }
}
