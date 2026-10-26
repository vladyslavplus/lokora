package com.vladyslavplus.lokora.reservation.async;

import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;

@Component
public class ReservationNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(ReservationNotificationListener.class);

    private final ReservationAsyncTasks asyncTasks;

    public ReservationNotificationListener(ReservationAsyncTasks asyncTasks) {
        this.asyncTasks = asyncTasks;
    }

    @EventListener
    public void onReservationOutcome(ReservationOutcomeEvent event) {
        try {
            asyncTasks.sendConfirmationNotice(event);
        } catch (TaskRejectedException rejected) {
            logger.warn("Simulated reservation notice skipped because executor is full: action={} reservationId={}",
                    event.action(), event.reservationId());
        }
    }
}
