package com.vladyslavplus.lokora.reservation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ReservationMaintenanceService {

    public static final int MAX_CLEANUP_BATCH_SIZE = 1_000;
    public static final int MAX_REPORT_WINDOW_DAYS = 31;

    private final ReservationRepository reservationRepository;
    private final Clock clock;
    private final ZoneId reportZone;

    public ReservationMaintenanceService(
            ReservationRepository reservationRepository,
            Clock clock,
            QuartzTimeZoneResolver timeZoneResolver,
            @Value("${lokora.quartz.time-zone:Europe/Kyiv}") String configuredTimeZone
    ) {
        this.reservationRepository = reservationRepository;
        this.clock = clock;
        this.reportZone = timeZoneResolver.resolve(configuredTimeZone);
    }

    @Transactional
    public int completeExpiredReservations(int batchSize) {
        if (batchSize < 1 || batchSize > MAX_CLEANUP_BATCH_SIZE) {
            throw new IllegalArgumentException("Cleanup batchSize must be between 1 and 1000");
        }

        List<Reservation> expiredReservations = reservationRepository.findExpiredActiveReservations(
                clock.instant(), PageRequest.of(0, batchSize)
        );
        expiredReservations.forEach(reservation -> reservation.setStatus(ReservationStatus.COMPLETED));
        return expiredReservations.size();
    }

    @Transactional(readOnly = true)
    public ResourceUtilizationDigest generateUtilizationDigest(int reportWindowDays) {
        if (reportWindowDays < 1 || reportWindowDays > MAX_REPORT_WINDOW_DAYS) {
            throw new IllegalArgumentException("reportWindowDays must be between 1 and 31");
        }

        LocalDate endDate = LocalDate.now(clock.withZone(reportZone));
        LocalDate startDate = endDate.minusDays(reportWindowDays);
        Instant windowStart = startDate.atStartOfDay(reportZone).toInstant();
        Instant windowEnd = endDate.atStartOfDay(reportZone).toInstant();

        List<ResourceUtilizationRow> resources = reservationRepository.summarizeUtilization(windowStart, windowEnd)
                .stream()
                .map(row -> new ResourceUtilizationRow(
                        row.getResourceId(),
                        row.getReservationCount(),
                        row.getOccupiedMinutes()
                ))
                .toList();

        return new ResourceUtilizationDigest(startDate, endDate, windowStart, windowEnd, resources);
    }
}
