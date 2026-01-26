package com.bsu.cvbuilder.annotation.agreement;

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
public class AgreementRequireAspect {

    private final SecurityService securityService;

    @Before("@annotation(agreementRequire)")
    public void beforeMethod(JoinPoint joinPoint, AgreementRequire agreementRequire) {
        UserProfile user = securityService.findCurrentUser();

        if (!user.isAgree()) {
            throw new AppException("You must agree with all conditions", 400);
        }
    }
}
