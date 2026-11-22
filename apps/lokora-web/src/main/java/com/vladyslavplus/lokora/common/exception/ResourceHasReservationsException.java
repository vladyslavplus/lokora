package com.vladyslavplus.lokora.common.exception;

public class ResourceHasReservationsException extends RuntimeException {

    public ResourceHasReservationsException() {
        super("Resource cannot be deleted while reservation history exists");
    }
}
