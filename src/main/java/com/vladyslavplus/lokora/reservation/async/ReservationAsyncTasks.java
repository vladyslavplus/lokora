package com.vladyslavplus.lokora.reservation.async;

import com.vladyslavplus.lokora.reservation.dto.ConfirmationStepResult;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ReservationAsyncTasks {

    private final ReservationTaskSimulator simulator;

    public ReservationAsyncTasks(ReservationTaskSimulator simulator) {
        this.simulator = simulator;
    }

    @Async("reservationTaskExecutor")
    public void sendConfirmationNotice(ReservationOutcomeEvent event) {
        simulator.sendConfirmationNotice(event);
    }

    @Async("reservationTaskExecutor")
    public CompletableFuture<ConfirmationStepResult> synchronizeCalendar(UUID reservationId) {
        return CompletableFuture.completedFuture(simulator.synchronizeCalendar(reservationId));
    }

    @Async("reservationTaskExecutor")
    public CompletableFuture<ConfirmationStepResult> prepareAccess(UUID reservationId) {
        return CompletableFuture.completedFuture(simulator.prepareAccess(reservationId));
    }
}
