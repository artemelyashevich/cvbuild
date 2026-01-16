package com.bsu.cvbuilder.annotation.limit;

import com.bsu.cvbuilder.service.LimitService;
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
public class LimitAspect {

    private final LimitService limitService;
    private final SecurityService securityService;

    @Before("@annotation(limited)")
    public void beforeMethod(JoinPoint joinPoint, Limited limited) {
        String userId = securityService.findCurrentUser().getId();
        
        log.debug("Checking limit {} for user {}", limited.value(), userId);

        limitService.check(userId, limited.value(), limited.capacity());
    }
}