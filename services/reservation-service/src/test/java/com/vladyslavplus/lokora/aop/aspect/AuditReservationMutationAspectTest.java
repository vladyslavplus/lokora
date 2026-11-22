package com.vladyslavplus.lokora.aop.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vladyslavplus.lokora.aop.annotation.AuditReservationMutation;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditReservationMutationAspectTest {

    private static final UUID RESERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RESOURCE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final Logger logger = (Logger) LoggerFactory.getLogger(AuditReservationMutationAspect.class);
    private final AuditReservationMutationAspect aspect = new AuditReservationMutationAspect();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void startLogCapture() {
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void stopLogCapture() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
        appender.stop();
    }

    @Test
    void auditSuccessRecordsActionAndReservationIdentifiers() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{new ReservationCreateRequest(
                RESOURCE_ID,
                Instant.parse("2026-10-10T10:00:00Z"),
                Instant.parse("2026-10-10T11:00:00Z")
        )});
        ReservationResponse response = new ReservationResponse(
                RESERVATION_ID,
                RESOURCE_ID,
                "subject-not-logged",
                Instant.parse("2026-10-10T10:00:00Z"),
                Instant.parse("2026-10-10T11:00:00Z"),
                ReservationStatus.ACTIVE
        );

        aspect.auditSuccess(joinPoint, annotation("create"), response);

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage()).contains("action=CREATE", "outcome=success");
            assertThat(event.getFormattedMessage()).contains(RESERVATION_ID.toString(), RESOURCE_ID.toString());
            assertThat(event.getFormattedMessage()).doesNotContain("subject-not-logged");
        });
    }

    @Test
    void auditFailureRecordsCauseTypeWithoutChangingIt() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{RESERVATION_ID});
        IllegalStateException failure = new IllegalStateException("failure detail is not logged");

        aspect.auditFailure(joinPoint, annotation("cancel"), failure);

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage()).contains("action=CANCEL", "outcome=failure");
            assertThat(event.getFormattedMessage()).contains(RESERVATION_ID.toString(), "cause=IllegalStateException");
            assertThat(event.getFormattedMessage()).doesNotContain("failure detail is not logged");
        });
    }

    private AuditReservationMutation annotation(String methodName) throws NoSuchMethodException {
        return AnnotatedMethods.class.getDeclaredMethod(methodName, methodName.equals("create")
                        ? ReservationCreateRequest.class
                        : UUID.class)
                .getAnnotation(AuditReservationMutation.class);
    }

    private static class AnnotatedMethods {
        @AuditReservationMutation(action = "CREATE")
        void create(ReservationCreateRequest request) {
        }

        @AuditReservationMutation(action = "CANCEL")
        void cancel(UUID id) {
        }
    }
}
