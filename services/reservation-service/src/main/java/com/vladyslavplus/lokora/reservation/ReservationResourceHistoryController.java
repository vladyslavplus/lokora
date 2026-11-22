package com.vladyslavplus.lokora.reservation;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations/internal/resources")
public class ReservationResourceHistoryController {
    private final ReservationRepository reservationRepository;

    public ReservationResourceHistoryController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/{resourceId}/has-history")
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public ReservationResourceHistoryResponse hasHistory(@PathVariable UUID resourceId) {
        return new ReservationResourceHistoryResponse(reservationRepository.existsByResourceId(resourceId));
    }

    public record ReservationResourceHistoryResponse(boolean hasHistory) {
    }
}
