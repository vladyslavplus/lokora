package com.vladyslavplus.lokora.reservation;

import com.vladyslavplus.lokora.common.api.GlobalExceptionHandler;
import com.vladyslavplus.lokora.common.exception.ReservationOverlapException;
import com.vladyslavplus.lokora.common.exception.ReservationNotFoundException;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.dto.ReservationPageResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private ReservationService reservationService;

    private MockMvc mockMvc;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReservationController(reservationService))
                .setControllerAdvice(new GlobalExceptionHandler(Clock.systemUTC()))
                .setValidator(new SpringValidatorAdapter(validator))
                .build();
    }

    @Test
    void createReturns201WithBodyAndWithoutMisleadingLocation() throws Exception {
        UUID id = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant start = Instant.parse("2026-09-22T10:00:00Z");
        Instant end = Instant.parse("2026-09-22T11:00:00Z");
        when(reservationService.create(any(ReservationCreateRequest.class))).thenReturn(
                new ReservationResponse(id, resourceId, "user-1", start, end, ReservationStatus.ACTIVE)
        );

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": "%s",
                                  "startTime": "2026-09-22T10:00:00Z",
                                  "endTime": "2026-09-22T11:00:00Z"
                                }
                                """.formatted(resourceId)))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void overlapReturns409WithStableErrorContract() throws Exception {
        when(reservationService.create(any(ReservationCreateRequest.class)))
                .thenThrow(new ReservationOverlapException());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": "11111111-1111-1111-1111-111111111111",
                                  "startTime": "2026-09-22T10:00:00Z",
                                  "endTime": "2026-09-22T11:00:00Z"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_OVERLAP"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/reservations"));
    }

    @Test
    void invalidReservationWindowReturns400WithConstraintMessageWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": "11111111-1111-1111-1111-111111111111",
                                  "startTime": "2026-10-10T10:00:00Z",
                                  "endTime": "2026-10-10T10:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("endTime must be after startTime")));

        verify(reservationService, never()).create(any(ReservationCreateRequest.class));
    }

    @Test
    void listMineReturns200() throws Exception {
        when(reservationService.listMine(0, null)).thenReturn(
                new ReservationPageResponse(List.of(), 0, 20, 0, 0)
        );

        mockMvc.perform(get("/api/reservations/mine"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelRouteReturns404ForUnknownReservation() throws Exception {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(reservationService.cancel(id)).thenThrow(new ReservationNotFoundException());

        mockMvc.perform(post("/api/reservations/{id}/cancel", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
