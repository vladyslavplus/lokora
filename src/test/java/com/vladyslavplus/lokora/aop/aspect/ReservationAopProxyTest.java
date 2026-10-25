package com.vladyslavplus.lokora.aop.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vladyslavplus.lokora.aop.annotation.MeasureExecutionTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationAopProxyTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(MeasureExecutionTimeAspect.class);
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
    void externalCallIsAdvisedAndSameMethodCalledThroughThisIsNot() {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new SelfInvocationProbe());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(new MeasureExecutionTimeAspect());
        SelfInvocationProbe proxy = proxyFactory.getProxy();

        assertThat(proxy.measuredOperation()).isEqualTo("measured");
        assertThat(proxy.invokeInternally()).isEqualTo("measured");

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage()).contains("measuredOperation");
    }

    public static class SelfInvocationProbe {

        @MeasureExecutionTime
        public String measuredOperation() {
            return "measured";
        }

        public String invokeInternally() {
            return this.measuredOperation();
        }
    }
}
