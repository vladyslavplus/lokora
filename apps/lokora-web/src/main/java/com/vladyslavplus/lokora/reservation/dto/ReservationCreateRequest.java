package com.vladyslavplus.lokora.reservation.dto;

import com.vladyslavplus.lokora.reservation.validation.ValidReservationWindow;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@ValidReservationWindow
public record ReservationCreateRequest(
        @NotNull UUID resourceId,
        @NotNull Instant startTime,
        @NotNull Instant endTime
) {
}
