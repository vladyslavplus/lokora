package com.vladyslavplus.lokora.reservation.async;

import com.vladyslavplus.lokora.reservation.ReservationService;
import com.vladyslavplus.lokora.reservation.dto.ConfirmationStepResult;
import com.vladyslavplus.lokora.reservation.dto.ReservationConfirmationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ReservationConfirmationService {

    private final ReservationService reservationService;
    private final ReservationAsyncTasks asyncTasks;
    private final Duration timeout;

    public ReservationConfirmationService(
            ReservationService reservationService,
            ReservationAsyncTasks asyncTasks,
            @Value("${lokora.async.confirmation-timeout:3s}") Duration timeout
    ) {
        this.reservationService = reservationService;
        this.asyncTasks = asyncTasks;
        this.timeout = timeout;
    }

    public CompletableFuture<ReservationConfirmationResponse> prepare(UUID reservationId) {
        reservationService.assertCanPrepareConfirmation(reservationId);

        CompletableFuture<ConfirmationStepResult> calendarSync = asyncTasks.synchronizeCalendar(reservationId)
                .exceptionally(failure -> ConfirmationStepResult.failed("calendarSync"))
                .completeOnTimeout(
                        ConfirmationStepResult.timedOut("calendarSync"), timeout.toMillis(), TimeUnit.MILLISECONDS
                );
        CompletableFuture<ConfirmationStepResult> accessPreparation = asyncTasks.prepareAccess(reservationId)
                .exceptionally(failure -> ConfirmationStepResult.failed("accessPreparation"))
                .completeOnTimeout(
                        ConfirmationStepResult.timedOut("accessPreparation"), timeout.toMillis(), TimeUnit.MILLISECONDS
                );

        return calendarSync.thenCombine(accessPreparation,
                (calendar, access) -> new ReservationConfirmationResponse(reservationId, calendar, access));
    }
}
