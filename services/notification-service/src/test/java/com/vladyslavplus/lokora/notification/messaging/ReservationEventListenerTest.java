package com.vladyslavplus.lokora.notification.messaging;

import com.vladyslavplus.lokora.contracts.reservation.ReservationEventType;
import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;
import com.vladyslavplus.lokora.notification.NotificationService;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReservationEventListenerTest {

    private static final ReservationLifecycleEvent EVENT = new ReservationLifecycleEvent(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            ReservationEventType.CREATED,
            Instant.parse("2026-10-01T00:00:00Z"),
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "employee-subject",
            Instant.parse("2026-10-10T10:00:00Z"),
            Instant.parse("2026-10-10T11:00:00Z"),
            ReservationStatus.ACTIVE
    );

    @Test
    void onReservationEvent_delegatesToNotificationService() {
        NotificationService service = mock(NotificationService.class);

        new ReservationEventListener(service).onReservationEvent(EVENT);

        verify(service).recordReservationEvent(EVENT);
    }

    @Test
    void onReservationEvent_listensToConfiguredQueueProperty() throws NoSuchMethodException {
        Method method = ReservationEventListener.class.getMethod(
                "onReservationEvent", ReservationLifecycleEvent.class
        );

        RabbitListener listener = method.getAnnotation(RabbitListener.class);

        assertThat(listener).isNotNull();
        assertThat(listener.queues()).containsExactly("${lokora.messaging.reservation-events.queue}");
    }
}
