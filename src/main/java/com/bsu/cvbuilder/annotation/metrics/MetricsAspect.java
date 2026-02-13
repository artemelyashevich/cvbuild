package com.bsu.cvbuilder.annotation.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MetricsAspect {

    private final MeterRegistry registry;

    public MetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(monitored)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        Timer.Sample sample = Timer.start(registry);

        String exceptionClass = "none";
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            exceptionClass = ex.getClass().getSimpleName();
            throw ex;
        } finally {
            stopTimer(sample, joinPoint, monitored, exceptionClass);
        }
    }

    private void stopTimer(Timer.Sample sample, ProceedingJoinPoint joinPoint, Monitored monitored, String exceptionClass) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String context = monitored.context().isEmpty() ? "default" : monitored.context();

        sample.stop(Timer.builder(monitored.value())
                .description("Execution time of " + className + "." + methodName)
                .tag("class", className)
                .tag("method", methodName)
                .tag("context", context)
                .tag("exception", exceptionClass)
                .publishPercentiles(0.95, 0.99)
                .register(registry));
    }
}