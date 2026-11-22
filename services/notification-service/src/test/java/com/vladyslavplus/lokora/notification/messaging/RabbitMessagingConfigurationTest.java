package com.vladyslavplus.lokora.notification.messaging;

import com.vladyslavplus.lokora.contracts.reservation.ReservationEventType;
import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.MessageConverter;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMessagingConfigurationTest {

    private static final NotificationMessagingProperties PROPERTIES = new NotificationMessagingProperties(
            "lokora.reservation.events",
            "notification-service.reservation-events",
            "reservation.*",
            "lokora.reservation.events.dlx",
            "notification-service.reservation-events.dlq"
    );

    private final RabbitMessagingConfiguration configuration = new RabbitMessagingConfiguration();

    @Test
    void reservationEventsQueue_isDurableAndDeadLettersRejectedMessages() {
        Queue queue = configuration.reservationEventsQueue(PROPERTIES);

        assertThat(queue.getName()).isEqualTo("notification-service.reservation-events");
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "lokora.reservation.events.dlx")
                .containsEntry("x-dead-letter-routing-key", "notification-service.reservation-events.dlq");
    }

    @Test
    void reservationEventsBinding_bindsQueueToDurableTopicExchangeWithPattern() {
        Binding binding = configuration.reservationEventsBinding(PROPERTIES);

        assertThat(binding.getExchange()).isEqualTo("lokora.reservation.events");
        assertThat(binding.getDestination()).isEqualTo("notification-service.reservation-events");
        assertThat(binding.getRoutingKey()).isEqualTo("reservation.*");
        assertThat(configuration.reservationEventsExchange(PROPERTIES).isDurable()).isTrue();
    }

    @Test
    void converter_roundTripsLifecycleEventAsPersistentJson() {
        MessageConverter converter = configuration.reservationEventMessageConverter();
        ReservationLifecycleEvent event = new ReservationLifecycleEvent(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                ReservationEventType.COMPLETED,
                Instant.parse("2026-10-01T00:00:00Z"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "employee-subject",
                Instant.parse("2026-10-10T10:00:00Z"),
                Instant.parse("2026-10-10T11:00:00Z"),
                ReservationStatus.COMPLETED
        );

        Message message = converter.toMessage(event, new MessageProperties());
        Object restored = converter.fromMessage(message);

        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(restored).isEqualTo(event);
    }
}
