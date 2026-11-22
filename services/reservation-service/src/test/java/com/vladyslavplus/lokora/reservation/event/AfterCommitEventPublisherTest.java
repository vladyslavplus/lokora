package com.vladyslavplus.lokora.reservation.event;

import com.vladyslavplus.lokora.reservation.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AfterCommitEventPublisherTest {

    private static final ReservationOutcomeEvent EVENT = new ReservationOutcomeEvent(
            "CREATED",
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            ReservationStatus.ACTIVE,
            "employee-subject",
            Instant.parse("2026-10-10T10:00:00Z"),
            Instant.parse("2026-10-10T11:00:00Z")
    );

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AfterCommitEventPublisher publisher = new AfterCommitEventPublisher(eventPublisher);
    private final TransactionTemplate transactionTemplate = new TransactionTemplate(new TestTransactionManager());

    @Test
    void publish_committedTransaction_publishesOnlyAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(EVENT);
            verifyNoInteractions(eventPublisher);
        });

        verify(eventPublisher).publishEvent(EVENT);
    }

    @Test
    void publish_rolledBackTransaction_neverPublishes() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(EVENT);
            throw new IllegalStateException("business action failed");
        })).isInstanceOf(IllegalStateException.class);

        verify(eventPublisher, never()).publishEvent(EVENT);
    }

    @Test
    void publish_markedRollbackOnly_neverPublishes() {
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(EVENT);
            status.setRollbackOnly();
        });

        verify(eventPublisher, never()).publishEvent(EVENT);
    }

    @Test
    void publish_withoutTransaction_publishesImmediately() {
        publisher.publish(EVENT);

        verify(eventPublisher).publishEvent(EVENT);
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
