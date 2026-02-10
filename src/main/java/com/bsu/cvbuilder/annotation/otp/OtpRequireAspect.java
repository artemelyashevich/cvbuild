package com.bsu.cvbuilder.annotation.otp;

import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.OtpService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OtpRequireAspect {

    private final SecurityService securityService;
    private final OtpService otpService;

    @Before("@annotation(otpRequire)")
    public void beforeMethod(JoinPoint joinPoint, OtpRequire otpRequire) {
        if (!otpRequire.value()) {
            return;
        }
        UserProfile user = securityService.findCurrentUser();
        log.debug("Check if otp is present for user: {}", user.getLogin());
        if (!otpService.exists(user.getLogin())) {
            log.info("OTP NOT FOUND for user: {}", user.getLogin());
            throw new AppException("This functionality require otp code", 401);
        }
        log.info("OTP FOUND for user: {}", user.getLogin());
    }
}
