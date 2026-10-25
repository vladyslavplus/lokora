package com.vladyslavplus.lokora.aop.aspect;

import com.vladyslavplus.lokora.aop.annotation.PublishReservationOutcome;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

class PublishReservationOutcomeAspectTest {

    private static final UUID RESERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RESOURCE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final PublishReservationOutcomeAspect aspect = new PublishReservationOutcomeAspect(eventPublisher);
    private final JoinPoint joinPoint = mock(JoinPoint.class);
    private final PublishReservationOutcome annotation = new AnnotatedMethods().getClass()
            .getDeclaredMethods()[0].getAnnotation(PublishReservationOutcome.class);

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishesTypedEventWhenNoTransactionIsActive() {
        aspect.publishAfterSuccess(joinPoint, annotation, response());

        verify(eventPublisher).publishEvent(new ReservationOutcomeEvent(
                "CREATED", RESERVATION_ID, RESOURCE_ID, ReservationStatus.ACTIVE
        ));
    }

    @Test
    void defersEventUntilTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();

        aspect.publishAfterSuccess(joinPoint, annotation, response());

        verify(eventPublisher, never()).publishEvent(any(ReservationOutcomeEvent.class));
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();

        verify(eventPublisher).publishEvent(new ReservationOutcomeEvent(
                "CREATED", RESERVATION_ID, RESOURCE_ID, ReservationStatus.ACTIVE
        ));
    }

    @Test
    void doesNotPublishAnOutcomeForNonReservationReturnValue() {
        aspect.publishAfterSuccess(joinPoint, annotation, "not a reservation response");

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void eventListenerFailureAfterCommitDoesNotChangeCompletedMutation() {
        TransactionSynchronizationManager.initSynchronization();
        doThrow(new IllegalStateException("listener failure"))
                .when(eventPublisher).publishEvent(any(ReservationOutcomeEvent.class));

        aspect.publishAfterSuccess(joinPoint, annotation, response());

        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
    }

    private ReservationResponse response() {
        return new ReservationResponse(
                RESERVATION_ID,
                RESOURCE_ID,
                "employee-subject",
                Instant.parse("2026-10-10T10:00:00Z"),
                Instant.parse("2026-10-10T11:00:00Z"),
                ReservationStatus.ACTIVE
        );
    }

    private static class AnnotatedMethods {
        @PublishReservationOutcome(action = "CREATED")
        void create() {
        }
    }
}
