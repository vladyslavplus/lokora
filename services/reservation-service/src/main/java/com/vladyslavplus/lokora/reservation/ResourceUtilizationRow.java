package com.vladyslavplus.lokora.reservation;

import java.util.UUID;

public record ResourceUtilizationRow(UUID resourceId, long reservationCount, long occupiedMinutes) {
}
