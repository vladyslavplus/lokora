package com.vladyslavplus.lokora.aop.aspect;

import com.vladyslavplus.lokora.aop.annotation.AuditReservationMutation;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class AuditReservationMutationAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditReservationMutationAspect.class);

    @AfterReturning(pointcut = "@annotation(audit)", returning = "result")
    public void auditSuccess(JoinPoint joinPoint, AuditReservationMutation audit, Object result) {
        ReservationIds ids = reservationIds(joinPoint.getArgs(), result);
        logger.info("Reservation mutation: action={} outcome=success reservationId={} resourceId={}",
                audit.action(), ids.reservationId(), ids.resourceId());
    }

    @AfterThrowing(pointcut = "@annotation(audit)", throwing = "failure")
    public void auditFailure(JoinPoint joinPoint, AuditReservationMutation audit, Throwable failure) {
        ReservationIds ids = reservationIds(joinPoint.getArgs(), null);
        logger.warn("Reservation mutation: action={} outcome=failure reservationId={} resourceId={} cause={}",
                audit.action(), ids.reservationId(), ids.resourceId(), failure.getClass().getSimpleName());
    }

    private ReservationIds reservationIds(Object[] arguments, Object result) {
        UUID reservationId = null;
        UUID resourceId = null;

        if (result instanceof ReservationResponse response) {
            reservationId = response.id();
            resourceId = response.resourceId();
        }

        for (Object argument : arguments) {
            if (argument instanceof ReservationCreateRequest request) {
                resourceId = request.resourceId();
            } else if (argument instanceof UUID id) {
                reservationId = id;
            }
        }

        return new ReservationIds(reservationId, resourceId);
    }

    private record ReservationIds(UUID reservationId, UUID resourceId) {
    }
}
