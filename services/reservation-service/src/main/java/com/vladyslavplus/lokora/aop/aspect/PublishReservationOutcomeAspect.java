package com.vladyslavplus.lokora.aop.aspect;

import com.vladyslavplus.lokora.aop.annotation.PublishReservationOutcome;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import com.vladyslavplus.lokora.reservation.event.AfterCommitEventPublisher;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PublishReservationOutcomeAspect {

    private final AfterCommitEventPublisher afterCommitEventPublisher;

    public PublishReservationOutcomeAspect(AfterCommitEventPublisher afterCommitEventPublisher) {
        this.afterCommitEventPublisher = afterCommitEventPublisher;
    }

    @AfterReturning(pointcut = "@annotation(publishOutcome)", returning = "result")
    public void publishAfterSuccess(
            JoinPoint joinPoint,
            PublishReservationOutcome publishOutcome,
            Object result
    ) {
        if (!(result instanceof ReservationResponse response)) {
            return;
        }

        afterCommitEventPublisher.publish(new ReservationOutcomeEvent(
                publishOutcome.action(),
                response.id(),
                response.resourceId(),
                response.status(),
                response.userId(),
                response.startTime(),
                response.endTime()
        ));
    }
}
