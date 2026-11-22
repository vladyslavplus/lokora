package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.reservation.dto.ReservationConfirmationResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.dto.ReservationPageResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationBffController {
    private final ReservationApplicationClient reservationClient;

    public ReservationBffController(ReservationApplicationClient reservationClient) {
        this.reservationClient = reservationClient;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationClient.create(request));
    }

    @GetMapping("/mine")
    public ReservationPageResponse listMine(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(required = false) Integer size) {
        return reservationClient.listMine(page, size);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable UUID id) {
        return reservationClient.cancel(id);
    }

    @PostMapping("/{id}/confirmation")
    public ReservationConfirmationResponse prepareConfirmation(@PathVariable UUID id) {
        return reservationClient.prepareConfirmation(id);
    }
}
