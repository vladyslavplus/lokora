package com.vladyslavplus.lokora.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationMaintenanceServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-15T12:00:00Z");

    @Mock
    private ReservationRepository reservationRepository;

    private ReservationMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new ReservationMaintenanceService(
                reservationRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                new QuartzTimeZoneResolver(),
                "Europe/Kyiv"
        );
    }

    @Test
    void cleanupMarksOnlyExpiredActiveRowsCompletedAndRepeatChangesNothing() {
        Reservation expired = reservation(ReservationStatus.ACTIVE);
        AtomicInteger cleanupReads = new AtomicInteger();
        when(reservationRepository.findExpiredActiveReservations(eq(FIXED_NOW), any(Pageable.class)))
                .thenAnswer(invocation -> cleanupReads.getAndIncrement() == 0 ? List.of(expired) : List.of());

        int firstRun = service.completeExpiredReservations(50);
        int secondRun = service.completeExpiredReservations(50);

        assertThat(firstRun).isEqualTo(1);
        assertThat(secondRun).isZero();
        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        verify(reservationRepository, times(2)).findExpiredActiveReservations(FIXED_NOW, PageRequest.of(0, 50));
    }

    @Test
    void cleanupRejectsUnboundedOrNonPositiveBatchSizes() {
        assertThatThrownBy(() -> service.completeExpiredReservations(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.completeExpiredReservations(ReservationMaintenanceService.MAX_CLEANUP_BATCH_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);

        verify(reservationRepository, never()).findExpiredActiveReservations(any(), any());
    }

    @Test
    void digestUsesKyivMidnightAndReturnsStableProjectionValues() {
        ResourceUtilizationProjection projection = org.mockito.Mockito.mock(ResourceUtilizationProjection.class);
        UUID resourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(projection.getResourceId()).thenReturn(resourceId);
        when(projection.getReservationCount()).thenReturn(3L);
        when(projection.getOccupiedMinutes()).thenReturn(120L);
        when(reservationRepository.summarizeUtilization(any(), any()))
                .thenAnswer(invocation -> List.of(projection));

        ResourceUtilizationDigest digest = service.generateUtilizationDigest(1);
        ResourceUtilizationDigest repeatedDigest = service.generateUtilizationDigest(1);

        assertThat(digest.startDate()).hasToString("2026-05-14");
        assertThat(digest.endDateExclusive()).hasToString("2026-05-15");
        assertThat(digest.windowStart()).isEqualTo(Instant.parse("2026-05-13T21:00:00Z"));
        assertThat(digest.windowEnd()).isEqualTo(Instant.parse("2026-05-14T21:00:00Z"));
        assertThat(digest.resources()).containsExactly(new ResourceUtilizationRow(resourceId, 3, 120));
        assertThat(repeatedDigest).isEqualTo(digest);
        verify(reservationRepository, times(2)).summarizeUtilization(digest.windowStart(), digest.windowEnd());
    }

    @Test
    void digestRejectsTooWideWindows() {
        assertThatThrownBy(() -> service.generateUtilizationDigest(32))
                .isInstanceOf(IllegalArgumentException.class);

        verify(reservationRepository, never()).summarizeUtilization(any(), any());
    }

    private Reservation reservation(ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        reservation.setStatus(status);
        reservation.setEndTime(FIXED_NOW.minusSeconds(30));
        return reservation;
    }
}
