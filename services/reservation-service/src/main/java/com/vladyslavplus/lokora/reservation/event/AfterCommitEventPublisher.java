package com.vladyslavplus.lokora.reservation.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes an application event only after the surrounding transaction commits.
 * A rolled-back transaction never publishes. Without an active transaction the event is published immediately.
 */
@Component
public class AfterCommitEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AfterCommitEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;

    public AfterCommitEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(ReservationOutcomeEvent event) {
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
