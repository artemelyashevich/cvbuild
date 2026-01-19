package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.dto.auth.EmailVerificationDto;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final SecurityService securityService;

    @PostMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    public void verify(@RequestBody EmailVerificationDto emailVerificationDto) {
        securityService.checkOtp(emailVerificationDto.otp());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void verifyEmail()  {
        securityService.verifyEmailRequest();
    }
}
