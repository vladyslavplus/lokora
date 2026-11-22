package com.vladyslavplus.lokora.reservation.messaging;

import com.vladyslavplus.lokora.contracts.reservation.ReservationEventType;
import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Forwards post-commit reservation outcomes to the message broker.
 * The upstream {@link ReservationOutcomeEvent} is only published after a successful commit.
 */
@Component
public class ReservationEventBridge {

    private static final Logger logger = LoggerFactory.getLogger(ReservationEventBridge.class);

    private final ReservationEventPublisher publisher;
    private final Clock clock;

    public ReservationEventBridge(ReservationEventPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    @EventListener
    public void onReservationOutcome(ReservationOutcomeEvent outcome) {
        ReservationEventType eventType;
        try {
            eventType = ReservationEventType.valueOf(outcome.action());
        } catch (IllegalArgumentException unknownAction) {
            logger.warn("Reservation outcome has no broker event type and was skipped: action={} reservationId={}",
                    outcome.action(), outcome.reservationId());
            return;
        }

        publisher.publish(new ReservationLifecycleEvent(
                UUID.randomUUID(),
                eventType,
                clock.instant(),
                outcome.reservationId(),
                outcome.resourceId(),
                outcome.userId(),
                outcome.startTime(),
                outcome.endTime(),
                outcome.status()
        ));
    }
}
