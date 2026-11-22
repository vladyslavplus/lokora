package com.vladyslavplus.lokora.reservation.async;

import com.vladyslavplus.lokora.reservation.dto.ConfirmationStepResult;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class ReservationTaskSimulator {

    private static final Logger logger = LoggerFactory.getLogger(ReservationTaskSimulator.class);

    private final Duration simulationDelay;

    public ReservationTaskSimulator(
            @Value("${lokora.async.simulation-delay:1s}") Duration simulationDelay
    ) {
        this.simulationDelay = simulationDelay;
    }

    public void sendConfirmationNotice(ReservationOutcomeEvent event) {
        simulate("confirmation-notice-" + event.action().toLowerCase(), event.reservationId());
    }

    public ConfirmationStepResult synchronizeCalendar(UUID reservationId) {
        simulate("calendar-sync", reservationId);
        return ConfirmationStepResult.completed("calendarSync");
    }

    public ConfirmationStepResult prepareAccess(UUID reservationId) {
        simulate("access-preparation", reservationId);
        return ConfirmationStepResult.completed("accessPreparation");
    }

    private void simulate(String operation, UUID reservationId) {
        try {
            Thread.sleep(simulationDelay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Reservation simulation was interrupted", interrupted);
        }
        logger.info("Simulated reservation side effect completed: operation={} reservationId={}",
                operation, reservationId);
    }
}
