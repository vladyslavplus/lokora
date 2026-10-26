package com.vladyslavplus.lokora.reservation.async;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import com.vladyslavplus.lokora.reservation.dto.ConfirmationStepResult;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.TaskRejectedException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class ReservationAsyncTasksTest {

    private static final UUID RESERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final ReservationOutcomeEvent EVENT = new ReservationOutcomeEvent(
            "CREATED",
            RESERVATION_ID,
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            ReservationStatus.ACTIVE
    );

    @Test
    void twoAsyncOperationsRunConcurrentlyOnNamedExecutorAndReturnResults() throws Exception {
        ReservationTaskSimulator simulator = mock(ReservationTaskSimulator.class);
        CountDownLatch bothTasksStarted = new CountDownLatch(2);
        CountDownLatch releaseTasks = new CountDownLatch(1);
        AtomicReference<String> calendarThread = new AtomicReference<>();
        AtomicReference<String> accessThread = new AtomicReference<>();
        when(simulator.synchronizeCalendar(RESERVATION_ID)).thenAnswer(invocation -> {
            calendarThread.set(Thread.currentThread().getName());
            bothTasksStarted.countDown();
            if (!releaseTasks.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Test gate timed out");
            }
            return ConfirmationStepResult.completed("calendarSync");
        });
        when(simulator.prepareAccess(RESERVATION_ID)).thenAnswer(invocation -> {
            accessThread.set(Thread.currentThread().getName());
            bothTasksStarted.countDown();
            if (!releaseTasks.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Test gate timed out");
            }
            return ConfirmationStepResult.completed("accessPreparation");
        });

        try (AnnotationConfigApplicationContext context = context(simulator, 2, 2, 1)) {
            ReservationAsyncTasks tasks = context.getBean(ReservationAsyncTasks.class);
            CompletableFuture<ConfirmationStepResult> calendar = tasks.synchronizeCalendar(RESERVATION_ID);
            CompletableFuture<ConfirmationStepResult> access = tasks.prepareAccess(RESERVATION_ID);

            assertThat(bothTasksStarted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(calendarThread.get()).startsWith("lokora-async-");
            assertThat(accessThread.get()).startsWith("lokora-async-");
            assertThat(calendarThread.get()).isNotEqualTo(accessThread.get());

            releaseTasks.countDown();
            assertThat(calendar.thenCombine(access, (left, right) -> left.status() + "/" + right.status())
                    .get(1, TimeUnit.SECONDS)).isEqualTo("COMPLETED/COMPLETED");
        } finally {
            releaseTasks.countDown();
        }
    }

    @Test
    void fireAndForgetReturnsBeforeNoticeFinishesAndVoidFailureIsReported() throws Exception {
        ReservationTaskSimulator simulator = mock(ReservationTaskSimulator.class);
        CountDownLatch noticeStarted = new CountDownLatch(1);
        CountDownLatch releaseNotice = new CountDownLatch(1);
        doAnswer(invocation -> {
            noticeStarted.countDown();
            releaseNotice.await(2, TimeUnit.SECONDS);
            throw new IllegalStateException("sensitive diagnostic");
        }).when(simulator).sendConfirmationNotice(EVENT);

        Logger logger = (Logger) LoggerFactory.getLogger(AsyncProcessingConfig.class);
        AsyncLogAppender appender = new AsyncLogAppender();
        appender.setContext(logger.getLoggerContext());
        logger.addAppender(appender);
        appender.start();
        try (AnnotationConfigApplicationContext context = context(simulator, 1, 1, 1)) {
            ReservationAsyncTasks tasks = context.getBean(ReservationAsyncTasks.class);

            tasks.sendConfirmationNotice(EVENT);

            assertThat(noticeStarted.await(1, TimeUnit.SECONDS)).isTrue();
            releaseNotice.countDown();
            assertThat(appender.awaitLoggedFailure()).isTrue();
            assertThat(appender.list.getFirst().getFormattedMessage())
                    .contains("sendConfirmationNotice", "IllegalStateException")
                    .doesNotContain("sensitive diagnostic");
        } finally {
            releaseNotice.countDown();
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void boundedExecutorRejectsWorkWhenWorkersAndQueueAreFull() throws Exception {
        ReservationTaskSimulator simulator = mock(ReservationTaskSimulator.class);
        CountDownLatch firstNoticeStarted = new CountDownLatch(1);
        CountDownLatch releaseNotice = new CountDownLatch(1);
        doAnswer(invocation -> {
            firstNoticeStarted.countDown();
            releaseNotice.await(2, TimeUnit.SECONDS);
            return null;
        }).when(simulator).sendConfirmationNotice(any());

        try (AnnotationConfigApplicationContext context = context(simulator, 1, 1, 1)) {
            ReservationAsyncTasks tasks = context.getBean(ReservationAsyncTasks.class);
            tasks.sendConfirmationNotice(EVENT);
            assertThat(firstNoticeStarted.await(1, TimeUnit.SECONDS)).isTrue();

            tasks.sendConfirmationNotice(EVENT);

            assertThatThrownBy(() -> tasks.sendConfirmationNotice(EVENT))
                    .isInstanceOf(TaskRejectedException.class);
            assertThatThrownBy(() -> tasks.synchronizeCalendar(RESERVATION_ID))
                    .isInstanceOf(TaskRejectedException.class);
            releaseNotice.countDown();
        } finally {
            releaseNotice.countDown();
        }
    }

    private AnnotationConfigApplicationContext context(
            ReservationTaskSimulator simulator,
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity
    ) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                "async-test-properties",
                Map.of(
                        "lokora.async.core-pool-size", corePoolSize,
                        "lokora.async.max-pool-size", maxPoolSize,
                        "lokora.async.queue-capacity", queueCapacity
                )
        ));
        context.registerBean(ReservationTaskSimulator.class, () -> simulator);
        context.register(AsyncProcessingConfig.class, ReservationAsyncTasks.class);
        context.refresh();
        return context;
    }

    private static class AsyncLogAppender extends ListAppender<ILoggingEvent> {

        private final CountDownLatch loggedFailure = new CountDownLatch(1);

        @Override
        protected void append(ILoggingEvent event) {
            super.append(event);
            loggedFailure.countDown();
        }

        boolean awaitLoggedFailure() throws InterruptedException {
            return loggedFailure.await(1, TimeUnit.SECONDS);
        }
    }
}
