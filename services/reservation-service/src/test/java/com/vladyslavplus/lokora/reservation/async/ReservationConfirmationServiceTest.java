package com.vladyslavplus.lokora.reservation.async;

import com.vladyslavplus.lokora.reservation.ReservationService;
import com.vladyslavplus.lokora.reservation.dto.ConfirmationStepResult;
import com.vladyslavplus.lokora.reservation.dto.ReservationConfirmationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationConfirmationServiceTest {

    private static final UUID RESERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final ReservationService reservationService = mock(ReservationService.class);
    private final ReservationAsyncTasks asyncTasks = mock(ReservationAsyncTasks.class);
    private ReservationConfirmationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationConfirmationService(reservationService, asyncTasks, Duration.ofSeconds(1));
    }

    @Test
    void startsBothTasksBeforeReturningCombinedFuture() {
        CompletableFuture<ConfirmationStepResult> calendar = new CompletableFuture<>();
        CompletableFuture<ConfirmationStepResult> access = new CompletableFuture<>();
        when(asyncTasks.synchronizeCalendar(RESERVATION_ID)).thenReturn(calendar);
        when(asyncTasks.prepareAccess(RESERVATION_ID)).thenReturn(access);

        CompletableFuture<ReservationConfirmationResponse> response = service.prepare(RESERVATION_ID);

        verify(reservationService).assertCanPrepareConfirmation(RESERVATION_ID);
        verify(asyncTasks).synchronizeCalendar(RESERVATION_ID);
        verify(asyncTasks).prepareAccess(RESERVATION_ID);
        assertThat(response).isNotCompleted();

        calendar.complete(ConfirmationStepResult.completed("calendarSync"));
        access.complete(ConfirmationStepResult.completed("accessPreparation"));

        assertThat(response.join()).isEqualTo(new ReservationConfirmationResponse(
                RESERVATION_ID,
                ConfirmationStepResult.completed("calendarSync"),
                ConfirmationStepResult.completed("accessPreparation")
        ));
    }

    @Test
    void convertsAsyncFailureIntoAVisibleFailedStep() {
        when(asyncTasks.synchronizeCalendar(RESERVATION_ID))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("private details")));
        when(asyncTasks.prepareAccess(RESERVATION_ID))
                .thenReturn(CompletableFuture.completedFuture(ConfirmationStepResult.completed("accessPreparation")));

        ReservationConfirmationResponse response = service.prepare(RESERVATION_ID).join();

        assertThat(response.calendarSync()).isEqualTo(ConfirmationStepResult.failed("calendarSync"));
        assertThat(response.calendarSync().message()).doesNotContain("private details");
        assertThat(response.accessPreparation().status()).isEqualTo(ConfirmationStepResult.Status.COMPLETED);
    }

    @Test
    void marksEachStepThatExceedsTheTimeout() throws Exception {
        service = new ReservationConfirmationService(reservationService, asyncTasks, Duration.ofMillis(20));
        when(asyncTasks.synchronizeCalendar(RESERVATION_ID)).thenReturn(new CompletableFuture<>());
        when(asyncTasks.prepareAccess(RESERVATION_ID))
                .thenReturn(CompletableFuture.completedFuture(ConfirmationStepResult.completed("accessPreparation")));

        ReservationConfirmationResponse response = service.prepare(RESERVATION_ID)
                .get(1, TimeUnit.SECONDS);

        assertThat(response.calendarSync()).isEqualTo(ConfirmationStepResult.timedOut("calendarSync"));
        assertThat(response.accessPreparation().status()).isEqualTo(ConfirmationStepResult.Status.COMPLETED);
    }

    @Test
    void propagatesExecutorRejectionForTheApiToMapAsServiceUnavailable() {
        when(asyncTasks.synchronizeCalendar(RESERVATION_ID))
                .thenThrow(new TaskRejectedException("executor full"));

        assertThatThrownBy(() -> service.prepare(RESERVATION_ID))
                .isInstanceOf(TaskRejectedException.class);
    }
}
