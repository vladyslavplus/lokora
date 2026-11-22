package com.vladyslavplus.lokora.reservation.messaging;

import com.vladyslavplus.lokora.contracts.reservation.ReservationEventType;
import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ReservationEventBridgeTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final UUID RESERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RESOURCE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant START = Instant.parse("2026-10-10T10:00:00Z");
    private static final Instant END = Instant.parse("2026-10-10T11:00:00Z");

    private final ReservationEventPublisher publisher = mock(ReservationEventPublisher.class);
    private final ReservationEventBridge bridge =
            new ReservationEventBridge(publisher, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void onReservationOutcome_createdOutcome_publishesOneLifecycleEventWithAllFields() {
        bridge.onReservationOutcome(outcome("CREATED", ReservationStatus.ACTIVE));

        ReservationLifecycleEvent event = publishedEvent();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo(ReservationEventType.CREATED);
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(event.resourceId()).isEqualTo(RESOURCE_ID);
        assertThat(event.userId()).isEqualTo("employee-subject");
        assertThat(event.startTime()).isEqualTo(START);
        assertThat(event.endTime()).isEqualTo(END);
        assertThat(event.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void onReservationOutcome_cancelledAndCompleted_mapToMatchingEventTypes() {
        bridge.onReservationOutcome(outcome("CANCELLED", ReservationStatus.CANCELLED));
        bridge.onReservationOutcome(outcome("COMPLETED", ReservationStatus.COMPLETED));

        ArgumentCaptor<ReservationLifecycleEvent> captor = ArgumentCaptor.forClass(ReservationLifecycleEvent.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(ReservationLifecycleEvent::eventType)
                .containsExactly(ReservationEventType.CANCELLED, ReservationEventType.COMPLETED);
    }

    @Test
    void onReservationOutcome_eachOutcomeGetsItsOwnEventId() {
        bridge.onReservationOutcome(outcome("CREATED", ReservationStatus.ACTIVE));
        bridge.onReservationOutcome(outcome("CREATED", ReservationStatus.ACTIVE));

        ArgumentCaptor<ReservationLifecycleEvent> captor = ArgumentCaptor.forClass(ReservationLifecycleEvent.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        assertThat(captor.getAllValues().get(0).eventId()).isNotEqualTo(captor.getAllValues().get(1).eventId());
    }

    @Test
    void onReservationOutcome_unknownAction_publishesNothing() {
        bridge.onReservationOutcome(outcome("ARCHIVED", ReservationStatus.ACTIVE));

        verify(publisher, never()).publish(any());
    }

    private ReservationLifecycleEvent publishedEvent() {
        ArgumentCaptor<ReservationLifecycleEvent> captor = ArgumentCaptor.forClass(ReservationLifecycleEvent.class);
        verify(publisher).publish(captor.capture());
        return captor.getValue();
    }

    private ReservationOutcomeEvent outcome(String action, ReservationStatus status) {
        return new ReservationOutcomeEvent(action, RESERVATION_ID, RESOURCE_ID, status, "employee-subject", START, END);
    }
}
