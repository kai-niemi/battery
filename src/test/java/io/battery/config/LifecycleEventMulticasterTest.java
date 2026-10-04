package io.battery.config;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import io.battery.event.LifecycleEvent;

@Tag("unit-test")
public class LifecycleEventMulticasterTest {
    static class TestLifecycleEvent extends ApplicationEvent implements LifecycleEvent {
        TestLifecycleEvent(Object source) {
            super(source);
        }
    }

    static class TestOtherEvent extends ApplicationEvent {
        TestOtherEvent(Object source) {
            super(source);
        }
    }

    static class Listeners {
        final List<String> invocations = new CopyOnWriteArrayList<>();

        final CompletableFuture<Thread> otherEventThread = new CompletableFuture<>();

        volatile Thread lifecycleEventThread;

        @EventListener
        @Order(2)
        public void second(TestLifecycleEvent event) {
            invocations.add("second");
        }

        @EventListener
        @Order(1)
        public void first(TestLifecycleEvent event) {
            lifecycleEventThread = Thread.currentThread();
            invocations.add("first");
        }

        @EventListener
        public void other(TestOtherEvent event) {
            otherEventThread.complete(Thread.currentThread());
        }
    }

    @Configuration
    static class TestConfig {
        @Bean
        public LifecycleEventMulticaster applicationEventMulticaster() {
            return new LifecycleEventMulticaster(new SimpleAsyncTaskExecutor());
        }

        @Bean
        public Listeners listeners() {
            return new Listeners();
        }
    }

    @Test
    public void givenLifecycleEvent_expectSynchronousOrderedDelivery() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            Listeners listeners = context.getBean(Listeners.class);

            context.publishEvent(new TestLifecycleEvent(this));

            // Delivered before publishEvent returns, on the publishing thread and in listener order
            Assertions.assertThat(listeners.invocations).containsExactly("first", "second");
            Assertions.assertThat(listeners.lifecycleEventThread).isSameAs(Thread.currentThread());
        }
    }

    @Test
    public void givenOtherEvent_expectAsynchronousDelivery() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            Listeners listeners = context.getBean(Listeners.class);

            context.publishEvent(new TestOtherEvent(this));

            Assertions.assertThat(listeners.otherEventThread.get(5, TimeUnit.SECONDS))
                    .isNotSameAs(Thread.currentThread());
        }
    }
}
