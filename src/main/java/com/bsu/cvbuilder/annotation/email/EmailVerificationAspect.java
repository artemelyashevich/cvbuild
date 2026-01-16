package com.bsu.cvbuilder.annotation.email;


import com.bsu.cvbuilder.domain.entity.user.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
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
public class EmailVerificationAspect {

    private final SecurityService securityService;

    @SuppressWarnings("all")
    @Before("@annotation(emailVerification)")
    public void beforeMethod(JoinPoint joinPoint, EmailVerification emailVerification) {
        UserProfile user = securityService.findCurrentUser();

        log.debug("Checking email verification for user {}", user.getId());

        if (emailVerification.value()) {
            return;
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new AppException("This functionality allows for verified users, please provide and verify your email", 400);
        }

        if (!user.getEmailVerified()) {
            throw new AppException("This functionality allows for verified users", 400);
        }
    }
}
