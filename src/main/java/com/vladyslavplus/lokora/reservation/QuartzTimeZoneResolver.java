package com.vladyslavplus.lokora.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.ZoneId;

@Component
public class QuartzTimeZoneResolver {

    private static final Logger logger = LoggerFactory.getLogger(QuartzTimeZoneResolver.class);
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("GMT");

    public ZoneId resolve(String configuredZoneId) {
        try {
            return ZoneId.of(configuredZoneId);
        } catch (DateTimeException | NullPointerException invalidZone) {
            logger.warn("Unknown Quartz timezone '{}'; using GMT", configuredZoneId);
            return FALLBACK_ZONE;
        }
    }
}
