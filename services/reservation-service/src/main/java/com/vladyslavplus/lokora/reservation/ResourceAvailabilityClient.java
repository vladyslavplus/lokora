package com.vladyslavplus.lokora.reservation;

import java.util.UUID;

public interface ResourceAvailabilityClient {
    boolean isAvailable(UUID resourceId);
}
