package com.vladyslavplus.lokora.reservation.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the reservation event topology. The consumer declares the same exchange, queue and dead-letter queue
 * with identical arguments, so events are retained even when the consumer has never been started.
 */
@Configuration
@EnableConfigurationProperties(ReservationMessagingProperties.class)
public class RabbitMessagingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMessagingConfiguration.class);
    private static final String CONTRACT_PACKAGE = "com.vladyslavplus.lokora.contracts.reservation";

    @Bean
    TopicExchange reservationEventsExchange(ReservationMessagingProperties properties) {
        return ExchangeBuilder.topicExchange(properties.exchange()).durable(true).build();
    }

    @Bean
    DirectExchange reservationEventsDeadLetterExchange(ReservationMessagingProperties properties) {
        return ExchangeBuilder.directExchange(properties.deadLetterExchange()).durable(true).build();
    }

    @Bean
    Queue reservationEventsQueue(ReservationMessagingProperties properties) {
        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterQueue())
                .build();
    }

    @Bean
    Queue reservationEventsDeadLetterQueue(ReservationMessagingProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    Binding reservationEventsBinding(ReservationMessagingProperties properties) {
        return BindingBuilder.bind(reservationEventsQueue(properties))
                .to(reservationEventsExchange(properties))
                .with(properties.bindingKey());
    }

    @Bean
    Binding reservationEventsDeadLetterBinding(ReservationMessagingProperties properties) {
        return BindingBuilder.bind(reservationEventsDeadLetterQueue(properties))
                .to(reservationEventsDeadLetterExchange(properties))
                .with(properties.deadLetterQueue());
    }

    @Bean
    MessageConverter reservationEventMessageConverter() {
        return new JacksonJsonMessageConverter(CONTRACT_PACKAGE);
    }

    @Bean
    RabbitTemplateCustomizer unroutableReservationEventLogger() {
        return template -> template.setReturnsCallback(returned -> logger.error(
                "Reservation event was not routed to any queue: exchange={} routingKey={} replyCode={} messageId={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getMessage().getMessageProperties().getMessageId()
        ));
    }
}
