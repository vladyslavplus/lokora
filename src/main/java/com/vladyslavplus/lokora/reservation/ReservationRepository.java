package com.vladyslavplus.lokora.reservation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.resourceId = :resourceId
              AND r.status = com.vladyslavplus.lokora.reservation.ReservationStatus.ACTIVE
              AND r.startTime < :endTime
              AND r.endTime > :startTime
            """)
    boolean existsActiveOverlap(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    Page<Reservation> findAllByUserIdOrderByStartTimeDescIdDesc(String userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.status = com.vladyslavplus.lokora.reservation.ReservationStatus.ACTIVE
              AND r.endTime <= :cutoff
            ORDER BY r.endTime ASC, r.id ASC
            """)
    List<Reservation> findExpiredActiveReservations(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query(value = """
            SELECT r.resource_id AS "resourceId",
                   COUNT(*) AS "reservationCount",
                   CAST(FLOOR(SUM(EXTRACT(EPOCH FROM (
                       LEAST(r.end_time, :windowEnd) - GREATEST(r.start_time, :windowStart)
                   )) / 60)) AS BIGINT) AS "occupiedMinutes"
            FROM reservation r
            WHERE r.status IN ('ACTIVE', 'COMPLETED')
              AND r.start_time < :windowEnd
              AND r.end_time > :windowStart
            GROUP BY r.resource_id
            ORDER BY r.resource_id
            """, nativeQuery = true)
    List<ResourceUtilizationProjection> summarizeUtilization(
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );

    boolean existsByResourceId(UUID resourceId);
}
