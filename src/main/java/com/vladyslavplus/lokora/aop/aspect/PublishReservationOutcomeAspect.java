package com.vladyslavplus.lokora.aop.aspect;

import com.vladyslavplus.lokora.aop.annotation.PublishReservationOutcome;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
public class PublishReservationOutcomeAspect {

    private static final Logger logger = LoggerFactory.getLogger(PublishReservationOutcomeAspect.class);
    private final ApplicationEventPublisher eventPublisher;

    public PublishReservationOutcomeAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
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

        ReservationOutcomeEvent event = new ReservationOutcomeEvent(
                publishOutcome.action(),
                response.id(),
                response.resourceId(),
                response.status()
        );
        publishAfterCommit(event);
    }

    private void publishAfterCommit(ReservationOutcomeEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishSafely(event);
            }
        });
    }

    private void publishSafely(ReservationOutcomeEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (RuntimeException failure) {
            logger.error("Reservation outcome event publication failed: action={} reservationId={}",
                    event.action(), event.reservationId(), failure);
        }
    }
}
