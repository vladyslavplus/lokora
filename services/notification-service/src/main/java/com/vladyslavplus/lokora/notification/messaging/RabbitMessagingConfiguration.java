package com.vladyslavplus.lokora.notification.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the same reservation event topology as the publisher, with identical arguments,
 * so the queue exists and keeps events even while this service is stopped.
 */
@Configuration
@EnableConfigurationProperties(NotificationMessagingProperties.class)
public class RabbitMessagingConfiguration {

    private static final String CONTRACT_PACKAGE = "com.vladyslavplus.lokora.contracts.reservation";

    @Bean
    TopicExchange reservationEventsExchange(NotificationMessagingProperties properties) {
        return ExchangeBuilder.topicExchange(properties.exchange()).durable(true).build();
    }

    @Bean
    DirectExchange reservationEventsDeadLetterExchange(NotificationMessagingProperties properties) {
        return ExchangeBuilder.directExchange(properties.deadLetterExchange()).durable(true).build();
    }

    @Bean
    Queue reservationEventsQueue(NotificationMessagingProperties properties) {
        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterQueue())
                .build();
    }

    @Bean
    Queue reservationEventsDeadLetterQueue(NotificationMessagingProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    Binding reservationEventsBinding(NotificationMessagingProperties properties) {
        return BindingBuilder.bind(reservationEventsQueue(properties))
                .to(reservationEventsExchange(properties))
                .with(properties.bindingKey());
    }

    @Bean
    Binding reservationEventsDeadLetterBinding(NotificationMessagingProperties properties) {
        return BindingBuilder.bind(reservationEventsDeadLetterQueue(properties))
                .to(reservationEventsDeadLetterExchange(properties))
                .with(properties.deadLetterQueue());
    }

    @Bean
    MessageConverter reservationEventMessageConverter() {
        return new JacksonJsonMessageConverter(CONTRACT_PACKAGE);
    }
}
