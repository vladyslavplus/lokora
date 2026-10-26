package com.vladyslavplus.lokora.reservation;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-15T12:00:00Z");
    private static final UUID RESOURCE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String USER_SUBJECT = "employee-subject";

    @Mock
    private WorkspaceResourceRepository resourceRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationMapper mapper;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new ReservationService(
                resourceRepository,
                reservationRepository,
                mapper,
                currentUserAccessor,
                clock
        );
    }

    @Test
    void createPersistsActiveReservationWithAuthenticatedSubject() {
        Instant start = FIXED_NOW;
        Instant end = FIXED_NOW.plusSeconds(3600);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(false);
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Reservation.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        ReservationResponse response = service.create(new ReservationCreateRequest(RESOURCE_ID, start, end));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(saved.getUserId()).isEqualTo(USER_SUBJECT);
        assertThat(saved.getStartTime()).isEqualTo(start);
        assertThat(saved.getEndTime()).isEqualTo(end);
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(response.status()).isEqualTo(ReservationStatus.ACTIVE);
        verify(resourceRepository).findByIdForUpdate(RESOURCE_ID);
    }

    @Test
    void createThrowsWhenResourceNotFound() {
        when(resourceRepository.findByIdForUpdate(RESOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(createRequest(FIXED_NOW, FIXED_NOW.plusSeconds(60))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenResourceInMaintenance() {
        stubResourceWithStatus(ResourceStatus.MAINTENANCE);

        assertThatThrownBy(() -> service.create(createRequest(FIXED_NOW, FIXED_NOW.plusSeconds(60))))
                .isInstanceOf(ResourceUnavailableForReservationException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenResourceOutOfService() {
        stubResourceWithStatus(ResourceStatus.OUT_OF_SERVICE);

        assertThatThrownBy(() -> service.create(createRequest(FIXED_NOW, FIXED_NOW.plusSeconds(60))))
                .isInstanceOf(ResourceUnavailableForReservationException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenStartEqualsEnd() {
        assertThatThrownBy(() -> service.create(createRequest(FIXED_NOW, FIXED_NOW)))
                .isInstanceOf(InvalidReservationPeriodException.class);

        verify(resourceRepository, never()).findByIdForUpdate(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenStartAfterEnd() {
        assertThatThrownBy(() -> service.create(createRequest(FIXED_NOW.plusSeconds(120), FIXED_NOW.plusSeconds(60))))
                .isInstanceOf(InvalidReservationPeriodException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenStartInPast() {
        assertThatThrownBy(() -> service.create(createRequest(FIXED_NOW.minusSeconds(1), FIXED_NOW.plusSeconds(60))))
                .isInstanceOf(InvalidReservationPeriodException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createAllowsStartExactlyAtFixedNow() {
        Instant start = FIXED_NOW;
        Instant end = FIXED_NOW.plusSeconds(1800);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(false);
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Reservation.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        service.create(new ReservationCreateRequest(RESOURCE_ID, start, end));

        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createThrowsOnLeftOverlap() {
        Instant start = FIXED_NOW.plusSeconds(1800);
        Instant end = FIXED_NOW.plusSeconds(5400);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ReservationCreateRequest(RESOURCE_ID, start, end)))
                .isInstanceOf(ReservationOverlapException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createThrowsOnRightOverlap() {
        Instant start = FIXED_NOW;
        Instant end = FIXED_NOW.plusSeconds(5400);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ReservationCreateRequest(RESOURCE_ID, start, end)))
                .isInstanceOf(ReservationOverlapException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenRequestedIntervalContainsExistingReservation() {
        Instant start = FIXED_NOW;
        Instant end = FIXED_NOW.plusSeconds(7200);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ReservationCreateRequest(RESOURCE_ID, start, end)))
                .isInstanceOf(ReservationOverlapException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenRequestedIntervalIsContainedByExistingReservation() {
        Instant start = FIXED_NOW.plusSeconds(1800);
        Instant end = FIXED_NOW.plusSeconds(3600);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ReservationCreateRequest(RESOURCE_ID, start, end)))
                .isInstanceOf(ReservationOverlapException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createThrowsOnIdenticalIntervalOverlap() {
        Instant start = FIXED_NOW.plusSeconds(3600);
        Instant end = FIXED_NOW.plusSeconds(7200);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ReservationCreateRequest(RESOURCE_ID, start, end)))
                .isInstanceOf(ReservationOverlapException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createAllowsAdjacentEarlierInterval() {
        Instant start = FIXED_NOW;
        Instant end = FIXED_NOW.plusSeconds(3600);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(false);
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Reservation.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        service.create(new ReservationCreateRequest(RESOURCE_ID, start, end));

        verify(reservationRepository).existsActiveOverlap(RESOURCE_ID, start, end);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createAllowsAdjacentLaterInterval() {
        Instant start = FIXED_NOW.plusSeconds(3600);
        Instant end = FIXED_NOW.plusSeconds(7200);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(false);
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Reservation.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        service.create(new ReservationCreateRequest(RESOURCE_ID, start, end));

        verify(reservationRepository).existsActiveOverlap(RESOURCE_ID, start, end);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createSucceedsWhenOverlapQueryExcludesCancelledReservations() {
        Instant start = FIXED_NOW.plusSeconds(3600);
        Instant end = FIXED_NOW.plusSeconds(7200);
        stubAvailableResource();
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, start, end)).thenReturn(false);
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Reservation.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        service.create(new ReservationCreateRequest(RESOURCE_ID, start, end));

        verify(reservationRepository).existsActiveOverlap(RESOURCE_ID, start, end);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void listOwnPassesExactSubjectToRepository() {
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        Reservation reservation = activeReservation(USER_SUBJECT, FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
        when(reservationRepository.findAllByUserIdOrderByStartTimeDescIdDesc(eq(USER_SUBJECT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reservation), PageRequest.of(0, 20), 1));
        when(mapper.toResponse(reservation)).thenReturn(toResponse(reservation));

        ReservationPageResponse response = service.listMine(0, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservationRepository).findAllByUserIdOrderByStartTimeDescIdDesc(eq(USER_SUBJECT), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(
                Sort.by("startTime").descending().and(Sort.by("id").descending())
        );
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().userId()).isEqualTo(USER_SUBJECT);
    }

    @Test
    void listOwnReturnsEmptyPageWhenRepositoryIsEmpty() {
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.findAllByUserIdOrderByStartTimeDescIdDesc(eq(USER_SUBJECT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        ReservationPageResponse response = service.listMine(0, null);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void listOwnUsesDefaultPageSizeAndDeterministicSort() {
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.findAllByUserIdOrderByStartTimeDescIdDesc(eq(USER_SUBJECT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.listMine(0, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservationRepository).findAllByUserIdOrderByStartTimeDescIdDesc(eq(USER_SUBJECT), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
    }

    @Test
    void listOwnAppliesRequestedPageAndSize() {
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.findAllByUserIdOrderByStartTimeDescIdDesc(eq(USER_SUBJECT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 10), 25));

        ReservationPageResponse response = service.listMine(2, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservationRepository).findAllByUserIdOrderByStartTimeDescIdDesc(eq(USER_SUBJECT), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(25);
    }

    @Test
    void listOwnCapsPageSizeAtMaximum() {
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.findAllByUserIdOrderByStartTimeDescIdDesc(eq(USER_SUBJECT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        service.listMine(0, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservationRepository).findAllByUserIdOrderByStartTimeDescIdDesc(eq(USER_SUBJECT), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void cancelAllowsOwnerToCancelFutureActiveReservation() {
        Reservation reservation = activeReservation(USER_SUBJECT, FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(currentUserAccessor.isWorkspaceAdmin()).thenReturn(false);
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(mapper.toResponse(reservation)).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        ReservationResponse response = service.cancel(RESERVATION_ID);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository).findByIdForUpdate(RESERVATION_ID);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelAllowsOwnerToCancelInProgressActiveReservation() {
        Reservation reservation = activeReservation(
                USER_SUBJECT,
                FIXED_NOW.minusSeconds(600),
                FIXED_NOW.plusSeconds(600)
        );
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(mapper.toResponse(reservation)).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        service.cancel(RESERVATION_ID);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelAllowsAdminToCancelAnotherUsersReservation() {
        Reservation reservation = activeReservation("other-user", FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(currentUserAccessor.isWorkspaceAdmin()).thenReturn(true);
        when(reservationRepository.save(reservation)).thenReturn(reservation);
        when(mapper.toResponse(reservation)).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        service.cancel(RESERVATION_ID);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelThrowsWhenReservationMissing() {
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(RESERVATION_ID))
                .isInstanceOf(ReservationNotFoundException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelThrowsForNonOwnerEmployee() {
        Reservation reservation = activeReservation("other-user", FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(currentUserAccessor.isWorkspaceAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.cancel(RESERVATION_ID))
                .isInstanceOf(ReservationForbiddenException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelThrowsWhenReservationAlreadyCancelled() {
        Reservation reservation = activeReservation(USER_SUBJECT, FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.cancel(RESERVATION_ID))
                .isInstanceOf(ReservationStateConflictException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelThrowsWhenReservationAlreadyEnded() {
        Reservation reservation = activeReservation(
                USER_SUBJECT,
                FIXED_NOW.minusSeconds(7200),
                FIXED_NOW.minusSeconds(3600)
        );
        when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));

        assertThatThrownBy(() -> service.cancel(RESERVATION_ID))
                .isInstanceOf(ReservationStateConflictException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void confirmationAccessAllowsReservationOwner() {
        Reservation reservation = activeReservation(USER_SUBJECT, FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));

        service.assertCanPrepareConfirmation(RESERVATION_ID);

        verify(currentUserAccessor, never()).isWorkspaceAdmin();
    }

    @Test
    void confirmationAccessAllowsWorkspaceAdmin() {
        Reservation reservation = activeReservation("another-employee", FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(currentUserAccessor.isWorkspaceAdmin()).thenReturn(true);

        service.assertCanPrepareConfirmation(RESERVATION_ID);

        verify(currentUserAccessor).isWorkspaceAdmin();
    }

    @Test
    void confirmationAccessRejectsAnotherEmployeesReservation() {
        Reservation reservation = activeReservation("another-employee", FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of(USER_SUBJECT));
        when(currentUserAccessor.isWorkspaceAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.assertCanPrepareConfirmation(RESERVATION_ID))
                .isInstanceOf(ReservationForbiddenException.class);
    }

    @Test
    void confirmationAccessRejectsCancelledReservation() {
        Reservation reservation = activeReservation(USER_SUBJECT, FIXED_NOW.plusSeconds(3600), FIXED_NOW.plusSeconds(7200));
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.assertCanPrepareConfirmation(RESERVATION_ID))
                .isInstanceOf(ReservationStateConflictException.class)
                .hasMessage("Only active reservations can be confirmed");
    }

    private ReservationCreateRequest createRequest(Instant start, Instant end) {
        return new ReservationCreateRequest(RESOURCE_ID, start, end);
    }

    private void stubAvailableResource() {
        stubResourceWithStatus(ResourceStatus.AVAILABLE);
    }

    private void stubResourceWithStatus(ResourceStatus status) {
        WorkspaceResource resource = mock(WorkspaceResource.class);
        when(resource.getId()).thenReturn(RESOURCE_ID);
        when(resource.getStatus()).thenReturn(status);
        when(resourceRepository.findByIdForUpdate(RESOURCE_ID)).thenReturn(Optional.of(resource));
    }

    private Reservation activeReservation(String userId, Instant start, Instant end) {
        Reservation reservation = new Reservation();
        reservation.setId(RESERVATION_ID);
        reservation.setResourceId(RESOURCE_ID);
        reservation.setUserId(userId);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setStatus(ReservationStatus.ACTIVE);
        return reservation;
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getResourceId(),
                reservation.getUserId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus()
        );
    }
}
