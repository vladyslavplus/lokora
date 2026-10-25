package com.vladyslavplus.lokora.reservation.validation;

import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ReservationWindowValidator.class)
public @interface ValidReservationWindow {

    String message() default "endTime must be after startTime";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
