package com.vladyslavplus.lokora.reservation.event;

import com.vladyslavplus.lokora.reservation.ReservationStatus;

import java.util.UUID;

public record ReservationOutcomeEvent(
        String action,
        UUID reservationId,
        UUID resourceId,
        ReservationStatus status
) {
}
