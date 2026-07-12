package com.loopin.api.common.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Aspect
@Component
@Order(0)
public class LoopinOperationMetricsAspect {

    private final LoopinMetrics loopinMetrics;

    public LoopinOperationMetricsAspect(LoopinMetrics loopinMetrics) {
        this.loopinMetrics = loopinMetrics;
    }

    @Around("@annotation(operation)")
    public Object observe(ProceedingJoinPoint joinPoint, LoopinOperation operation) throws Throwable {
        long startNanos = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            record(operation, true, startNanos);
            return result;
        } catch (Throwable exception) {
            record(operation, false, startNanos);
            throw exception;
        }
    }

    private void record(LoopinOperation operation, boolean success, long startNanos) {
        loopinMetrics.recordOperation(
            operation.domain(),
            operation.operation(),
            success,
            Duration.ofNanos(System.nanoTime() - startNanos)
        );
    }
}
