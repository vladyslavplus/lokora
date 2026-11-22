package com.vladyslavplus.lokora.notification.messaging;

import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;
import com.vladyslavplus.lokora.notification.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventListener {

    private final NotificationService notificationService;

    public ReservationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${lokora.messaging.reservation-events.queue}")
    public void onReservationEvent(ReservationLifecycleEvent event) {
        notificationService.recordReservationEvent(event);
    }
}
