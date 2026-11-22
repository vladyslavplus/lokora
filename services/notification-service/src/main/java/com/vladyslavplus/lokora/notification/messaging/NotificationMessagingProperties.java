package com.vladyslavplus.lokora.notification.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lokora.messaging.reservation-events")
public record NotificationMessagingProperties(
        String exchange,
        String queue,
        String bindingKey,
        String deadLetterExchange,
        String deadLetterQueue
) {
}
