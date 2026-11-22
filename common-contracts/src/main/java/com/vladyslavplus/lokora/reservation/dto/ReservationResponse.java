package com.vladyslavplus.lokora.reservation.dto;

import com.vladyslavplus.lokora.reservation.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID resourceId,
        String userId,
        Instant startTime,
        Instant endTime,
        ReservationStatus status
) {
}
