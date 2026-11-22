package com.vladyslavplus.lokora.aop.aspect;

import com.vladyslavplus.lokora.aop.annotation.MeasureExecutionTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MeasureExecutionTimeAspect {

    private static final Logger logger = LoggerFactory.getLogger(MeasureExecutionTimeAspect.class);

    @Around("@annotation(measureExecutionTime)")
    public Object measure(ProceedingJoinPoint joinPoint, MeasureExecutionTime measureExecutionTime) throws Throwable {
        long startedAt = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedNanos = System.nanoTime() - startedAt;
            logger.info("Execution time measured: method={} elapsedNanos={}",
                    joinPoint.getSignature().toShortString(), elapsedNanos);
        }
    }
}
