package com.vladyslavplus.lokora.reservation.messaging;

import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;

public interface ReservationEventPublisher {

    void publish(ReservationLifecycleEvent event);
}
