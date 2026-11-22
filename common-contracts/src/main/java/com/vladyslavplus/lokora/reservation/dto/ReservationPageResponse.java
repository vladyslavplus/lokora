package com.vladyslavplus.lokora.reservation.dto;

import java.util.List;

public record ReservationPageResponse(
        List<ReservationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
