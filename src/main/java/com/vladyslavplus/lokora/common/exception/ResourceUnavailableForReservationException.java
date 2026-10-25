package com.vladyslavplus.lokora.common.exception;

public class ResourceUnavailableForReservationException extends RuntimeException {

    public ResourceUnavailableForReservationException() {
        super("Resource is not available for reservation");
    }
}
