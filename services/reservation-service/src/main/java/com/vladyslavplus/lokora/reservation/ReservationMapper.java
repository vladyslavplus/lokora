package com.vladyslavplus.lokora.reservation;

import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    ReservationResponse toResponse(Reservation reservation);
}
