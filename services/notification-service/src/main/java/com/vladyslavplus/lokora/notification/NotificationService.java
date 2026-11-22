package com.vladyslavplus.lokora.notification;

import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;
import com.vladyslavplus.lokora.notification.dto.NotificationPageResponse;
import com.vladyslavplus.lokora.security.CurrentUserAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class NotificationService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final CurrentUserAccessor currentUserAccessor;
    private final Clock clock;

    public NotificationService(
            NotificationRepository repository,
            NotificationMapper mapper,
            CurrentUserAccessor currentUserAccessor,
            Clock clock
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUserAccessor = currentUserAccessor;
        this.clock = clock;
    }

    /**
     * Stores one notification per business event. Handling the same {@code eventId} again changes nothing.
     * Intentionally not transactional: a duplicate key violation must stay a local outcome of the single insert,
     * not mark a surrounding transaction rollback-only.
     *
     * @return {@code true} when the event was stored, {@code false} when it had already been processed
     */
    public boolean recordReservationEvent(ReservationLifecycleEvent event) {
        if (repository.existsByEventId(event.eventId())) {
            logger.info("Duplicate reservation event ignored: eventId={} eventType={}",
                    event.eventId(), event.eventType());
            return false;
        }

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setEventId(event.eventId());
        notification.setEventType(event.eventType());
        notification.setReservationId(event.reservationId());
        notification.setResourceId(event.resourceId());
        notification.setUserId(event.userId());
        notification.setMessage(messageFor(event));
        notification.setCreatedAt(clock.instant());

        try {
            repository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            logger.info("Duplicate reservation event ignored after concurrent insert: eventId={} eventType={}",
                    event.eventId(), event.eventType());
            return false;
        }
        logger.info("Reservation event stored: eventId={} eventType={} reservationId={}",
                event.eventId(), event.eventType(), event.reservationId());
        return true;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'WORKSPACE_ADMIN')")
    public NotificationPageResponse listMine(int page, Integer size) {
        String userId = currentUserAccessor.getSubject()
                .orElseThrow(() -> new IllegalStateException("Authenticated subject is required"));
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                resolvedPage,
                resolvedSize,
                Sort.by("createdAt").descending().and(Sort.by("id").descending())
        );
        Page<Notification> pageResult = repository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        return new NotificationPageResponse(
                pageResult.getContent().stream().map(mapper::toResponse).toList(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    private String messageFor(ReservationLifecycleEvent event) {
        return switch (event.eventType()) {
            case CREATED -> "Reservation confirmed: resource %s from %s to %s"
                    .formatted(event.resourceId(), event.startTime(), event.endTime());
            case CANCELLED -> "Reservation cancelled: resource %s from %s to %s"
                    .formatted(event.resourceId(), event.startTime(), event.endTime());
            case COMPLETED -> "Reservation completed: resource %s, ended at %s"
                    .formatted(event.resourceId(), event.endTime());
        };
    }
}
