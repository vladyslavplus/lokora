package com.vladyslavplus.lokora.notification;

import com.vladyslavplus.lokora.contracts.reservation.ReservationEventType;
import com.vladyslavplus.lokora.contracts.reservation.ReservationLifecycleEvent;
import com.vladyslavplus.lokora.notification.dto.NotificationPageResponse;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import com.vladyslavplus.lokora.security.CurrentUserAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RESERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RESOURCE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private NotificationRepository repository;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    private final NotificationMapper mapper = notification -> null;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, mapper, currentUserAccessor, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordReservationEvent_newEvent_savesOneNotificationWithEventData() {
        when(repository.existsByEventId(EVENT_ID)).thenReturn(false);

        boolean stored = service.recordReservationEvent(event(ReservationEventType.CREATED));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).saveAndFlush(captor.capture());
        Notification saved = captor.getValue();
        assertThat(stored).isTrue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
        assertThat(saved.getEventType()).isEqualTo(ReservationEventType.CREATED);
        assertThat(saved.getReservationId()).isEqualTo(RESERVATION_ID);
        assertThat(saved.getResourceId()).isEqualTo(RESOURCE_ID);
        assertThat(saved.getUserId()).isEqualTo("employee-subject");
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getMessage()).startsWith("Reservation confirmed");
    }

    @Test
    void recordReservationEvent_eachEventType_getsItsOwnMessage() {
        when(repository.existsByEventId(any())).thenReturn(false);

        service.recordReservationEvent(event(ReservationEventType.CANCELLED));
        service.recordReservationEvent(event(ReservationEventType.COMPLETED));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).saveAndFlush(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getMessage)
                .satisfiesExactly(
                        message -> assertThat(message).startsWith("Reservation cancelled"),
                        message -> assertThat(message).startsWith("Reservation completed")
                );
    }

    @Test
    void recordReservationEvent_sameEventTwice_storesOnlyOnceAndResultStaysTheSame() {
        Map<UUID, Notification> store = new HashMap<>();
        when(repository.existsByEventId(any())).thenAnswer(invocation -> store.containsKey(invocation.getArgument(0)));
        when(repository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            store.put(notification.getEventId(), notification);
            return notification;
        });
        ReservationLifecycleEvent event = event(ReservationEventType.CREATED);

        boolean first = service.recordReservationEvent(event);
        Notification afterFirst = store.get(EVENT_ID);
        String messageAfterFirst = afterFirst.getMessage();
        boolean second = service.recordReservationEvent(event);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(store).hasSize(1);
        assertThat(store.get(EVENT_ID)).isSameAs(afterFirst);
        assertThat(store.get(EVENT_ID).getMessage()).isEqualTo(messageAfterFirst);
        verify(repository, times(1)).saveAndFlush(any(Notification.class));
    }

    @Test
    void recordReservationEvent_alreadyProcessedEvent_doesNotSaveAgain() {
        when(repository.existsByEventId(EVENT_ID)).thenReturn(true);

        boolean stored = service.recordReservationEvent(event(ReservationEventType.CREATED));

        assertThat(stored).isFalse();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void recordReservationEvent_concurrentDuplicateInsert_isTreatedAsAlreadyProcessed() {
        when(repository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(repository.saveAndFlush(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("uq_notification_event_id"));

        boolean stored = service.recordReservationEvent(event(ReservationEventType.CREATED));

        assertThat(stored).isFalse();
    }

    @Test
    void listMine_queriesRepositoryWithAuthenticatedSubjectAndBoundedPage() {
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of("employee-subject"));
        when(repository.findAllByUserIdOrderByCreatedAtDescIdDesc(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        NotificationPageResponse response = service.listMine(-5, 1_000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByUserIdOrderByCreatedAtDescIdDesc(
                org.mockito.ArgumentMatchers.eq("employee-subject"), pageable.capture()
        );
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(NotificationService.MAX_PAGE_SIZE);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void listMine_withoutAuthenticatedSubject_failsBeforeQueryingRepository() {
        when(currentUserAccessor.getSubject()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listMine(0, null)).isInstanceOf(IllegalStateException.class);

        verify(repository, never()).findAllByUserIdOrderByCreatedAtDescIdDesc(any(), any());
    }

    private ReservationLifecycleEvent event(ReservationEventType type) {
        return new ReservationLifecycleEvent(
                EVENT_ID,
                type,
                NOW,
                RESERVATION_ID,
                RESOURCE_ID,
                "employee-subject",
                Instant.parse("2026-10-10T10:00:00Z"),
                Instant.parse("2026-10-10T11:00:00Z"),
                ReservationStatus.ACTIVE
        );
    }
}
