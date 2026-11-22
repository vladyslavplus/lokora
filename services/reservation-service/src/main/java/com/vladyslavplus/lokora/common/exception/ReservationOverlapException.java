package com.vladyslavplus.lokora.common.exception;

public class ReservationOverlapException extends RuntimeException {

    public ReservationOverlapException() {
        super("Reservation overlaps an existing active reservation");
    }
}
