package com.vladyslavplus.lokora.reservation.dto;

import java.util.UUID;

public record ReservationConfirmationResponse(
        UUID reservationId,
        ConfirmationStepResult calendarSync,
        ConfirmationStepResult accessPreparation
) {
}
