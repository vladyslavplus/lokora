package com.vladyslavplus.lokora.common.api;

import com.vladyslavplus.lokora.common.exception.InvalidReservationPeriodException;
import com.vladyslavplus.lokora.common.exception.ReservationForbiddenException;
import com.vladyslavplus.lokora.common.exception.ReservationNotFoundException;
import com.vladyslavplus.lokora.common.exception.ReservationOverlapException;
import com.vladyslavplus.lokora.common.exception.ReservationStateConflictException;
import com.vladyslavplus.lokora.common.exception.ResourceHasReservationsException;
import com.vladyslavplus.lokora.common.exception.ResourceNotFoundException;
import com.vladyslavplus.lokora.common.exception.ResourceUnavailableForReservationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return notFound(exception.getMessage(), request);
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationNotFound(
            ReservationNotFoundException exception,
            HttpServletRequest request
    ) {
        return notFound(exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidReservationPeriodException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidReservationPeriod(
            InvalidReservationPeriodException exception,
            HttpServletRequest request
    ) {
        return badRequest("INVALID_REQUEST", exception.getMessage(), request);
    }

    @ExceptionHandler(ReservationOverlapException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationOverlap(
            ReservationOverlapException exception,
            HttpServletRequest request
    ) {
        return conflict("RESERVATION_OVERLAP", exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceUnavailableForReservationException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceUnavailable(
            ResourceUnavailableForReservationException exception,
            HttpServletRequest request
    ) {
        return conflict("RESOURCE_UNAVAILABLE", exception.getMessage(), request);
    }

    @ExceptionHandler(ReservationStateConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationStateConflict(
            ReservationStateConflictException exception,
            HttpServletRequest request
    ) {
        return conflict("RESERVATION_STATE_CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceHasReservationsException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceHasReservations(
            ResourceHasReservationsException exception,
            HttpServletRequest request
    ) {
        return conflict("RESOURCE_HAS_RESERVATIONS", exception.getMessage(), request);
    }

    @ExceptionHandler(ReservationForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationForbidden(
            ReservationForbiddenException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(
                        "FORBIDDEN",
                        exception.getMessage(),
                        clock.instant(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return badRequest("VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return badRequest("INVALID_REQUEST", "Request body is malformed or unreadable", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = "Invalid value for parameter '" + exception.getName() + "'";
        return badRequest("INVALID_REQUEST", message, request);
    }

    private ResponseEntity<ApiErrorResponse> badRequest(
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        code,
                        message,
                        clock.instant(),
                        request.getRequestURI()
                ));
    }

    private ResponseEntity<ApiErrorResponse> notFound(String message, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        "NOT_FOUND",
                        message,
                        clock.instant(),
                        request.getRequestURI()
                ));
    }

    private ResponseEntity<ApiErrorResponse> conflict(
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        code,
                        message,
                        clock.instant(),
                        request.getRequestURI()
                ));
    }
}
