package com.vladyslavplus.lokora.reservation.async;

import com.vladyslavplus.lokora.reservation.ReservationStatus;
import com.vladyslavplus.lokora.reservation.event.ReservationOutcomeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReservationNotificationListenerTest {

    private static final ReservationOutcomeEvent EVENT = new ReservationOutcomeEvent(
            "CREATED",
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            ReservationStatus.ACTIVE
    );

    @Test
    void listenerSubscribesToTheExistingPostCommitOutcomeEvent() throws NoSuchMethodException {
        Method listenerMethod = ReservationNotificationListener.class.getMethod(
                "onReservationOutcome", ReservationOutcomeEvent.class
        );
        EventListener listener = listenerMethod.getAnnotation(EventListener.class);

        assertThat(listener).isNotNull();
    }

    @Test
    void rejectedNoticeIsLoggedByPolicyAndDoesNotEscapeCommittedEventHandler() {
        ReservationAsyncTasks asyncTasks = mock(ReservationAsyncTasks.class);
        doThrow(new TaskRejectedException("executor full"))
                .when(asyncTasks).sendConfirmationNotice(EVENT);
        ReservationNotificationListener listener = new ReservationNotificationListener(asyncTasks);

        assertThatCode(() -> listener.onReservationOutcome(EVENT)).doesNotThrowAnyException();
        verify(asyncTasks).sendConfirmationNotice(EVENT);
    }
}
