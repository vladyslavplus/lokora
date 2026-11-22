package com.vladyslavplus.lokora.reservation;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class QuartzTimeZoneResolverTest {

    private final QuartzTimeZoneResolver resolver = new QuartzTimeZoneResolver();

    @Test
    void retainsConfiguredZoneWhenItIsKnown() {
        assertThat(resolver.resolve("Europe/Kyiv")).isEqualTo(ZoneId.of("Europe/Kyiv"));
    }

    @Test
    void fallsBackToGmtForUnknownZone() {
        assertThat(resolver.resolve("Invalid/Unknown")).isEqualTo(ZoneId.of("GMT"));
    }
}
