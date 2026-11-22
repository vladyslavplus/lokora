package com.vladyslavplus.lokora.aop.aspect;

import com.vladyslavplus.lokora.common.exception.ReservationOverlapException;
import com.vladyslavplus.lokora.reservation.ReservationMapper;
import com.vladyslavplus.lokora.reservation.ReservationRepository;
import com.vladyslavplus.lokora.reservation.ReservationService;
import com.vladyslavplus.lokora.reservation.ResourceAvailabilityClient;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.event.AfterCommitEventPublisher;
import com.vladyslavplus.lokora.security.CurrentUserAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationEventNotPublishedOnFailureTest {

    private static final UUID RESOURCE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant START = Instant.parse("2026-10-10T10:00:00Z");
    private static final Instant END = Instant.parse("2026-10-10T11:00:00Z");

    @Test
    void create_overlappingReservation_failsWithoutPublishingAnyEvent() {
        ResourceAvailabilityClient availabilityClient = mock(ResourceAvailabilityClient.class);
        ReservationRepository repository = mock(ReservationRepository.class);
        CurrentUserAccessor currentUserAccessor = mock(CurrentUserAccessor.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        when(availabilityClient.isAvailable(RESOURCE_ID)).thenReturn(true);
        when(repository.existsActiveOverlap(RESOURCE_ID, START, END)).thenReturn(true);
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of("employee-subject"));

        ReservationService target = new ReservationService(
                availabilityClient,
                repository,
                mock(ReservationMapper.class),
                currentUserAccessor,
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
        );
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(new PublishReservationOutcomeAspect(new AfterCommitEventPublisher(eventPublisher)));
        ReservationService proxy = proxyFactory.getProxy();

        assertThatThrownBy(() -> proxy.create(new ReservationCreateRequest(RESOURCE_ID, START, END)))
                .isInstanceOf(ReservationOverlapException.class);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(repository, never()).save(any());
    }

    @Test
    void create_resourceUnavailable_failsWithoutPublishingAnyEvent() {
        ResourceAvailabilityClient availabilityClient = mock(ResourceAvailabilityClient.class);
        ReservationRepository repository = mock(ReservationRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        when(availabilityClient.isAvailable(RESOURCE_ID)).thenReturn(false);

        ReservationService target = new ReservationService(
                availabilityClient,
                repository,
                mock(ReservationMapper.class),
                mock(CurrentUserAccessor.class),
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
        );
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(new PublishReservationOutcomeAspect(new AfterCommitEventPublisher(eventPublisher)));
        ReservationService proxy = proxyFactory.getProxy();

        assertThatThrownBy(() -> proxy.create(new ReservationCreateRequest(RESOURCE_ID, START, END)))
                .isInstanceOf(com.vladyslavplus.lokora.common.exception.ResourceUnavailableForReservationException.class);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
