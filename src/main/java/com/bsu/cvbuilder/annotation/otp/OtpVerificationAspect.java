package com.bsu.cvbuilder.annotation.otp;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.OtpService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class OtpVerificationAspect {

    private final SecurityService securityService;
    private final OtpService otpService;
    private final NotificationService notificationService;

    @Around("@annotation(otpVerification)")
    public Object verifyOtp(ProceedingJoinPoint joinPoint, OtpVerification otpVerification) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String otp = null;

        for (Object arg : args) {
            if (arg instanceof String s) {
                otp = s;
                break;
            }
        }

        UserProfile user = securityService.findCurrentUser();
        String key = otpVerification.key().formatted(user.getLogin());

        if (otp == null) {
            String code = otpService.create(user, key);
            notificationService.sendNotification(NotificationDto.builder()
                    .templateName(otpVerification.template())
                    .parameters(Map.of("code", code))
                    .engine(otpVerification.engine())
                    .receiver(user.getEmail())
                    .build());
            return null;
        }

        if (!otpService.validateOtp(user, otp, key)) {
            throw new AppException("Invalid OTP", 401);
        }

        return joinPoint.proceed();
    }
}