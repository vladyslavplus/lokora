package com.vladyslavplus.lokora.aop.aspect;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vladyslavplus.lokora.reservation.Reservation;
import com.vladyslavplus.lokora.reservation.ReservationMapper;
import com.vladyslavplus.lokora.reservation.ReservationRepository;
import com.vladyslavplus.lokora.reservation.ReservationService;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import com.vladyslavplus.lokora.resource.ResourceStatus;
import com.vladyslavplus.lokora.resource.WorkspaceResource;
import com.vladyslavplus.lokora.resource.WorkspaceResourceRepository;
import com.vladyslavplus.lokora.security.CurrentUserAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceAopProxyTest {

    private static final UUID RESOURCE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant START = Instant.parse("2026-10-10T10:00:00Z");
    private static final Instant END = Instant.parse("2026-10-10T11:00:00Z");

    private final Logger measureLogger = (Logger) LoggerFactory.getLogger(MeasureExecutionTimeAspect.class);
    private final Logger auditLogger = (Logger) LoggerFactory.getLogger(AuditReservationMutationAspect.class);
    private final ListAppender<ILoggingEvent> measureAppender = new ListAppender<>();
    private final ListAppender<ILoggingEvent> auditAppender = new ListAppender<>();
    private ch.qos.logback.classic.Level measureLevel;
    private ch.qos.logback.classic.Level auditLevel;

    @BeforeEach
    void captureAspectLogs() {
        measureLevel = measureLogger.getLevel();
        auditLevel = auditLogger.getLevel();
        measureLogger.setLevel(ch.qos.logback.classic.Level.INFO);
        auditLogger.setLevel(ch.qos.logback.classic.Level.INFO);
        measureAppender.start();
        auditAppender.start();
        measureLogger.addAppender(measureAppender);
        auditLogger.addAppender(auditAppender);
    }

    @AfterEach
    void stopCapturingAspectLogs() {
        measureLogger.detachAppender(measureAppender);
        auditLogger.detachAppender(auditAppender);
        measureLogger.setLevel(measureLevel);
        auditLogger.setLevel(auditLevel);
        measureAppender.stop();
        auditAppender.stop();
    }

    @Test
    void proxyRunsAllThreeAspectsOnProductionReservationCreateMethod() {
        WorkspaceResourceRepository resourceRepository = mock(WorkspaceResourceRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        ReservationMapper mapper = mock(ReservationMapper.class);
        CurrentUserAccessor currentUserAccessor = mock(CurrentUserAccessor.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        WorkspaceResource resource = mock(WorkspaceResource.class);
        when(resource.getStatus()).thenReturn(ResourceStatus.AVAILABLE);
        when(resourceRepository.findByIdForUpdate(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(reservationRepository.existsActiveOverlap(RESOURCE_ID, START, END)).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId(RESERVATION_ID);
            return saved;
        });
        when(mapper.toResponse(any(Reservation.class))).thenReturn(
                new ReservationResponse(RESERVATION_ID, RESOURCE_ID, "employee-subject", START, END, ReservationStatus.ACTIVE)
        );
        when(currentUserAccessor.getSubject()).thenReturn(Optional.of("employee-subject"));

        ReservationService target = new ReservationService(
                resourceRepository,
                reservationRepository,
                mapper,
                currentUserAccessor,
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
        );
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(new MeasureExecutionTimeAspect());
        proxyFactory.addAspect(new AuditReservationMutationAspect());
        proxyFactory.addAspect(new PublishReservationOutcomeAspect(eventPublisher));

        ReservationService proxy = proxyFactory.getProxy();
        ReservationResponse result = proxy.create(new ReservationCreateRequest(RESOURCE_ID, START, END));

        assertThat(result.id()).isEqualTo(RESERVATION_ID);
        assertThat(measureAppender.list).hasSize(1);
        assertThat(measureAppender.list.getFirst().getFormattedMessage()).contains("create");
        assertThat(auditAppender.list).hasSize(1);
        assertThat(auditAppender.list.getFirst().getFormattedMessage())
                .contains("action=CREATE", "outcome=success", RESERVATION_ID.toString(), RESOURCE_ID.toString())
                .doesNotContain("employee-subject");
        verify(eventPublisher).publishEvent(new ReservationOutcomeEvent(
                "CREATED", RESERVATION_ID, RESOURCE_ID, ReservationStatus.ACTIVE
        ));
    }
}
