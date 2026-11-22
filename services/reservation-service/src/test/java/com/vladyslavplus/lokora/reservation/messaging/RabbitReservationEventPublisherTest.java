package com.vladyslavplus.lokora.reservation.messaging;

import com.vladyslavplus.lokora.contracts.reservation.ReservationEventType;
import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitReservationEventPublisherTest {

    private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final ReservationLifecycleEvent EVENT = new ReservationLifecycleEvent(
            EVENT_ID,
            ReservationEventType.CANCELLED,
            Instant.parse("2026-10-01T00:00:00Z"),
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "employee-subject",
            Instant.parse("2026-10-10T10:00:00Z"),
            Instant.parse("2026-10-10T11:00:00Z"),
            ReservationStatus.CANCELLED
    );

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ReservationMessagingProperties properties = new ReservationMessagingProperties(
            "lokora.reservation.events", "queue", "reservation.*", "dlx", "dlq"
    );
    private final RabbitReservationEventPublisher publisher =
            new RabbitReservationEventPublisher(rabbitTemplate, properties);

    @Test
    void publish_sendsEventToConfiguredExchangeWithRoutingKeyFromEventType() {
        publisher.publish(EVENT);

        verify(rabbitTemplate).convertAndSend(
                eq("lokora.reservation.events"),
                eq("reservation.cancelled"),
                eq((Object) EVENT),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    @Test
    void publish_setsMessageIdAndCorrelationToEventId() throws IOException {
        publisher.publish(EVENT);

        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CorrelationData> correlation = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(
                any(String.class), any(String.class), any(Object.class), processor.capture(), correlation.capture()
        );
        Message message = new Message(new byte[0], new MessageProperties());
        processor.getValue().postProcessMessage(message);

        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(EVENT_ID.toString());
        assertThat(correlation.getValue().getId()).isEqualTo(EVENT_ID.toString());
    }

    @Test
    void publish_brokerUnavailable_doesNotPropagateToCommittedBusinessAction() {
        doThrow(new AmqpConnectException(new IOException("broker down")))
                .when(rabbitTemplate).convertAndSend(
                        any(String.class), any(String.class), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class)
                );

        assertThatCode(() -> publisher.publish(EVENT)).doesNotThrowAnyException();
    }
}
