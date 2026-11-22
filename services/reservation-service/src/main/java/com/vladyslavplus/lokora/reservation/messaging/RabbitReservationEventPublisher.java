package com.vladyslavplus.lokora.reservation.messaging;

import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RabbitReservationEventPublisher implements ReservationEventPublisher {

    static final String ROUTING_KEY_PREFIX = "reservation.";

    private static final Logger logger = LoggerFactory.getLogger(RabbitReservationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ReservationMessagingProperties properties;

    public RabbitReservationEventPublisher(RabbitTemplate rabbitTemplate, ReservationMessagingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(ReservationLifecycleEvent event) {
        String routingKey = ROUTING_KEY_PREFIX + event.eventType().name().toLowerCase(Locale.ROOT);
        String messageId = event.eventId().toString();
        CorrelationData correlationData = new CorrelationData(messageId);
        correlationData.getFuture().whenComplete((confirm, failure) -> {
            if (failure != null || confirm == null || !confirm.isAck()) {
                logger.error("Reservation event was not confirmed by the broker: eventId={} eventType={}",
                        event.eventId(), event.eventType());
            }
        });

        try {
            rabbitTemplate.convertAndSend(properties.exchange(), routingKey, event, message -> {
                message.getMessageProperties().setMessageId(messageId);
                return message;
            }, correlationData);
        } catch (AmqpException failure) {
            logger.error("Reservation event publication failed: eventId={} eventType={}",
                    event.eventId(), event.eventType(), failure);
        }
    }
}
