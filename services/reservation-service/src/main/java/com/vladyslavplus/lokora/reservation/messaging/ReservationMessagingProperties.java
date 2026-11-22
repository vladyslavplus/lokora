package com.vladyslavplus.lokora.reservation.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lokora.messaging.reservation-events")
public record ReservationMessagingProperties(
        String exchange,
        String queue,
        String bindingKey,
        String deadLetterExchange,
        String deadLetterQueue
) {
}
