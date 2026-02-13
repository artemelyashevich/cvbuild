package com.bsu.cvbuilder.annotation.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MetricsAspect {

    private final PrometheusMetrics metricsService;

    public MetricsAspect(PrometheusMetrics metricsService) {
        this.metricsService = metricsService;
    }

    @Around("@annotation(monitored)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        try (var timer = metricsService.startTimer(monitored.value(), className, methodName, monitored.context())) {
            try {
                return joinPoint.proceed();
            } catch (Throwable ex) {
                timer.recordError(ex.getClass().getSimpleName());
                throw ex;
            }
        }
    }
}