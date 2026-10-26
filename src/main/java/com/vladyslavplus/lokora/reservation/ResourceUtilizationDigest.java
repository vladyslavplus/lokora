package com.vladyslavplus.lokora.reservation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ResourceUtilizationDigest(
        LocalDate startDate,
        LocalDate endDateExclusive,
        Instant windowStart,
        Instant windowEnd,
        List<ResourceUtilizationRow> resources
) {
    public ResourceUtilizationDigest {
        resources = List.copyOf(resources);
    }
}
