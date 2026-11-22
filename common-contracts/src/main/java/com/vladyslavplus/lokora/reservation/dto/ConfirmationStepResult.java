package com.vladyslavplus.lokora.reservation.dto;

public record ConfirmationStepResult(String operation, Status status, String message) {

    public enum Status {
        COMPLETED,
        FAILED,
        TIMED_OUT
    }

    public static ConfirmationStepResult completed(String operation) {
        return new ConfirmationStepResult(operation, Status.COMPLETED, "Completed");
    }

    public static ConfirmationStepResult failed(String operation) {
        return new ConfirmationStepResult(operation, Status.FAILED, "This step could not be completed");
    }

    public static ConfirmationStepResult timedOut(String operation) {
        return new ConfirmationStepResult(operation, Status.TIMED_OUT, "This step exceeded the time limit");
    }
}
