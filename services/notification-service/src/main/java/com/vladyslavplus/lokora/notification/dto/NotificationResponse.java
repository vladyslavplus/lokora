package com.vladyslavplus.lokora.notification.dto;

import com.vladyslavplus.lokora.contracts.reservation.ReservationEventType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        ReservationEventType eventType,
        UUID reservationId,
        UUID resourceId,
        String message,
        Instant createdAt
) {
}
