package com.vladyslavplus.lokora.common.exception;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException() {
        super("Reservation not found");
    }
}
