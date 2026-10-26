package com.vladyslavplus.lokora.reservation;

import java.util.UUID;

public interface ResourceUtilizationProjection {

    UUID getResourceId();

    Long getReservationCount();

    Long getOccupiedMinutes();
}
