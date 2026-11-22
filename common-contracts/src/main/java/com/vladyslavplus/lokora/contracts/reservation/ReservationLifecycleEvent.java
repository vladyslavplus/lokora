package com.vladyslavplus.lokora.contracts.reservation;

import com.vladyslavplus.lokora.reservation.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire contract of a reservation lifecycle event sent through the message broker.
 * {@code eventId} identifies the business event and stays the same on every redelivery.
 */
public record ReservationLifecycleEvent(
        UUID eventId,
        ReservationEventType eventType,
        Instant occurredAt,
        UUID reservationId,
        UUID resourceId,
        String userId,
        Instant startTime,
        Instant endTime,
        ReservationStatus status
) {
}
