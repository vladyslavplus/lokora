package com.vladyslavplus.lokora.reservation;

import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.dto.ReservationPageResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationConfirmationResponse;
import com.vladyslavplus.lokora.reservation.async.ReservationConfirmationService;
import com.vladyslavplus.lokora.web.annotation.LokoraRestController;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@LokoraRestController("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationConfirmationService confirmationService;

    public ReservationController(
            ReservationService reservationService,
            ReservationConfirmationService confirmationService
    ) {
        this.reservationService = reservationService;
        this.confirmationService = confirmationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request) {
        ReservationResponse created = reservationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/mine")
    public ReservationPageResponse listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size
    ) {
        return reservationService.listMine(page, size);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable UUID id) {
        return reservationService.cancel(id);
    }

    @PostMapping("/{id}/confirmation")
    public CompletableFuture<ReservationConfirmationResponse> prepareConfirmation(@PathVariable UUID id) {
        return confirmationService.prepare(id);
    }
}
