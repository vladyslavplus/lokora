package com.vladyslavplus.lokora.reservation.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncProcessingConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessingConfig.class);

    @Bean(name = "reservationTaskExecutor")
    public ThreadPoolTaskExecutor reservationTaskExecutor(
            @Value("${lokora.async.core-pool-size:2}") int corePoolSize,
            @Value("${lokora.async.max-pool-size:4}") int maxPoolSize,
            @Value("${lokora.async.queue-capacity:8}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("lokora-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (failure, method, arguments) -> logAsyncFailure(failure, method);
    }

    private void logAsyncFailure(Throwable failure, Method method) {
        logger.error("Async void task failed: method={} failureType={}",
                method.getName(), failure.getClass().getSimpleName());
    }
}
