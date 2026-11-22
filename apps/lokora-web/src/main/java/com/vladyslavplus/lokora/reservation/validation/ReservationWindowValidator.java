package com.vladyslavplus.lokora.reservation.validation;

import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReservationWindowValidator implements ConstraintValidator<ValidReservationWindow, ReservationCreateRequest> {

    @Override
    public boolean isValid(ReservationCreateRequest request, ConstraintValidatorContext context) {
        if (request == null || request.startTime() == null || request.endTime() == null) {
            return true;
        }

        if (request.startTime().isBefore(request.endTime())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endTime")
                .addConstraintViolation();
        return false;
    }
}
