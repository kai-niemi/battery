package io.battery.config;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    @Primary
    @Bean("asyncTaskExecutor")
    public AsyncTaskExecutor getAsyncExecutor() {
        // Use an unbounded virtual thread task executor for I/O bound workers.
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("async-");
        executor.setCancelRemainingTasksOnClose(true);
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(-1);
        return executor;
    }

    @Bean
    public AsyncTaskExecutor multicasterTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("async-mc-");
        executor.setCancelRemainingTasksOnClose(true);
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(-1);
        return executor;
    }

    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster(
            @Autowired @Qualifier("multicasterTaskExecutor") Executor executor) {
        // Lifecycle events are delivered synchronously, all other events asynchronously
        SimpleApplicationEventMulticaster eventMulticaster = new LifecycleEventMulticaster(executor);
        eventMulticaster.setErrorHandler(t -> {
            logger.error("Unexpected error occurred in scheduled task", t);
        });
        return eventMulticaster;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            logger.error("Unexpected exception occurred invoking async method: " + method, ex);
        };
    }

    @Bean
    public CallableProcessingInterceptor callableProcessingInterceptor() {
        return new TimeoutCallableProcessingInterceptor();
    }
}


