package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.reservation.dto.ReservationConfirmationResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.dto.ReservationPageResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;

import java.util.UUID;

public interface ReservationApplicationClient {
    ReservationPageResponse listMine(int page, Integer size);
    ReservationResponse create(ReservationCreateRequest request);
    ReservationResponse cancel(UUID id);
    ReservationConfirmationResponse prepareConfirmation(UUID id);
    boolean hasResourceReservations(UUID resourceId);
}
