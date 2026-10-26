package com.vladyslavplus.lokora.reservation;

import com.vladyslavplus.lokora.aop.annotation.AuditReservationMutation;
import com.vladyslavplus.lokora.aop.annotation.MeasureExecutionTime;
import com.vladyslavplus.lokora.aop.annotation.PublishReservationOutcome;
import com.vladyslavplus.lokora.common.exception.InvalidReservationPeriodException;
import com.vladyslavplus.lokora.common.exception.ReservationForbiddenException;
import com.vladyslavplus.lokora.common.exception.ReservationNotFoundException;
import com.vladyslavplus.lokora.common.exception.ReservationOverlapException;
import com.vladyslavplus.lokora.common.exception.ReservationStateConflictException;
import com.vladyslavplus.lokora.common.exception.ResourceNotFoundException;
import com.vladyslavplus.lokora.common.exception.ResourceUnavailableForReservationException;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.dto.ReservationPageResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import com.vladyslavplus.lokora.resource.ResourceStatus;
import com.vladyslavplus.lokora.resource.WorkspaceResource;
import com.vladyslavplus.lokora.resource.WorkspaceResourceRepository;
import com.vladyslavplus.lokora.security.CurrentUserAccessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ReservationService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final WorkspaceResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationMapper mapper;
    private final CurrentUserAccessor currentUserAccessor;
    private final Clock clock;

    public ReservationService(
            WorkspaceResourceRepository resourceRepository,
            ReservationRepository reservationRepository,
            ReservationMapper mapper,
            CurrentUserAccessor currentUserAccessor,
            Clock clock
    ) {
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.mapper = mapper;
        this.currentUserAccessor = currentUserAccessor;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'WORKSPACE_ADMIN')")
    @MeasureExecutionTime
    @AuditReservationMutation(action = "CREATE")
    @PublishReservationOutcome(action = "CREATED")
    public ReservationResponse create(ReservationCreateRequest request) {
        Instant now = clock.instant();
        Instant startTime = request.startTime();
        Instant endTime = request.endTime();

        validatePeriod(now, startTime, endTime);

        WorkspaceResource resource = resourceRepository.findByIdForUpdate(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace resource not found"));

        if (resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new ResourceUnavailableForReservationException();
        }

        if (reservationRepository.existsActiveOverlap(request.resourceId(), startTime, endTime)) {
            throw new ReservationOverlapException();
        }

        String userId = requireSubject();

        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setResourceId(request.resourceId());
        reservation.setUserId(userId);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatus.ACTIVE);

        Reservation saved = reservationRepository.save(reservation);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'WORKSPACE_ADMIN')")
    public ReservationPageResponse listMine(int page, Integer size) {
        String userId = requireSubject();
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                resolvedPage,
                resolvedSize,
                Sort.by("startTime").descending().and(Sort.by("id").descending())
        );
        Page<Reservation> pageResult = reservationRepository.findAllByUserIdOrderByStartTimeDescIdDesc(userId, pageable);
        return new ReservationPageResponse(
                pageResult.getContent().stream().map(mapper::toResponse).toList(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'WORKSPACE_ADMIN')")
    public void assertCanPrepareConfirmation(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(ReservationNotFoundException::new);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ReservationStateConflictException("Only active reservations can be confirmed");
        }
        requireOwnerOrWorkspaceAdmin(reservation);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'WORKSPACE_ADMIN')")
    @AuditReservationMutation(action = "CANCEL")
    @PublishReservationOutcome(action = "CANCELLED")
    public ReservationResponse cancel(UUID id) {
        Instant now = clock.instant();
        Reservation reservation = reservationRepository.findByIdForUpdate(id)
                .orElseThrow(ReservationNotFoundException::new);

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ReservationStateConflictException("Reservation is not active");
        }
        if (!reservation.getEndTime().isAfter(now)) {
            throw new ReservationStateConflictException("Reservation has already ended");
        }

        requireOwnerOrWorkspaceAdmin(reservation);

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);
        return mapper.toResponse(saved);
    }

    private void validatePeriod(Instant now, Instant startTime, Instant endTime) {
        if (!startTime.isBefore(endTime)) {
            if (startTime.equals(endTime)) {
                throw new InvalidReservationPeriodException("Start time must be before end time");
            }
            throw new InvalidReservationPeriodException("Start time must be before end time");
        }
        if (startTime.isBefore(now)) {
            throw new InvalidReservationPeriodException("Start time cannot be in the past");
        }
    }

    private String requireSubject() {
        return currentUserAccessor.getSubject()
                .orElseThrow(() -> new IllegalStateException("Authenticated subject is required"));
    }

    private void requireOwnerOrWorkspaceAdmin(Reservation reservation) {
        String subject = requireSubject();
        boolean owner = subject.equals(reservation.getUserId());
        if (!owner && !currentUserAccessor.isWorkspaceAdmin()) {
            throw new ReservationForbiddenException();
        }
    }
}
