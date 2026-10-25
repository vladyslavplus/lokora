package com.vladyslavplus.lokora.aop.aspect;

import com.vladyslavplus.lokora.aop.annotation.MeasureExecutionTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeasureExecutionTimeAspectTest {

    private final MeasureExecutionTimeAspect aspect = new MeasureExecutionTimeAspect();
    private final MeasureExecutionTime annotation = new AnnotatedMethods().getClass()
            .getDeclaredMethods()[0].getAnnotation(MeasureExecutionTime.class);

    @Test
    void proceedsExactlyOnceAndReturnsOriginalResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        configureSignature(joinPoint);
        Object result = new Object();
        when(joinPoint.proceed()).thenReturn(result);

        Object actual = aspect.measure(joinPoint, annotation);

        assertThat(actual).isSameAs(result);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void propagatesOriginalExceptionAndStillCompletesAdvice() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        configureSignature(joinPoint);
        IllegalStateException failure = new IllegalStateException("expected");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.measure(joinPoint, annotation)).isSameAs(failure);

        verify(joinPoint, times(1)).proceed();
    }

    private void configureSignature(ProceedingJoinPoint joinPoint) {
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("operation()");
    }

    private static class AnnotatedMethods {
        @MeasureExecutionTime
        void operation() {
        }
    }
}
