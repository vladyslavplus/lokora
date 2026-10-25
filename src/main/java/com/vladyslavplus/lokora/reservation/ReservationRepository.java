package com.vladyslavplus.lokora.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

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

    boolean existsByResourceId(UUID resourceId);
}
