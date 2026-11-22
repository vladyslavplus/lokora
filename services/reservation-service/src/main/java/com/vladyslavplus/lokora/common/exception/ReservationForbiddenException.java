package com.vladyslavplus.lokora.common.exception;

public class ReservationForbiddenException extends RuntimeException {

    public ReservationForbiddenException() {
        super("You are not allowed to cancel this reservation");
    }
}
