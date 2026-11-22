package com.vladyslavplus.lokora.reservation;

import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationCreateRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void validReservationWindowPassesWithoutViolation() {
        ReservationCreateRequest request = request(
                Instant.parse("2026-10-10T10:00:00Z"),
                Instant.parse("2026-10-10T11:00:00Z")
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void equalTimesProduceSpecificEndTimeViolation() {
        Instant instant = Instant.parse("2026-10-10T10:00:00Z");

        Set<ConstraintViolation<ReservationCreateRequest>> violations = validator.validate(request(instant, instant));

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("endTime");
            assertThat(violation.getMessage()).isEqualTo("endTime must be after startTime");
        });
    }

    @Test
    void reversedTimesProduceSpecificEndTimeViolation() {
        Set<ConstraintViolation<ReservationCreateRequest>> violations = validator.validate(request(
                Instant.parse("2026-10-10T11:00:00Z"),
                Instant.parse("2026-10-10T10:00:00Z")
        ));

        assertThat(violations).singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("endTime must be after startTime");
    }

    @Test
    void nullTimesAreHandledByExistingNotNullConstraints() {
        Set<ConstraintViolation<ReservationCreateRequest>> violations = validator.validate(
                new ReservationCreateRequest(UUID.randomUUID(), null, null)
        );

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("startTime", "endTime");
    }

    private ReservationCreateRequest request(Instant startTime, Instant endTime) {
        return new ReservationCreateRequest(UUID.randomUUID(), startTime, endTime);
    }
}
