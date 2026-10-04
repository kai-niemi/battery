package io.battery.config;

import java.util.concurrent.Executor;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.ResolvableType;

import io.battery.event.LifecycleEvent;

/**
 * Event multicaster delivering {@link LifecycleEvent}s synchronously on the publishing
 * thread, so that listeners observe them in publication order and listener order, and
 * all other events asynchronously using the task executor.
 */
public class LifecycleEventMulticaster extends SimpleApplicationEventMulticaster {
    public LifecycleEventMulticaster(Executor taskExecutor) {
        setTaskExecutor(taskExecutor);
    }

    @Override
    public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
        if (event instanceof LifecycleEvent) {
            ResolvableType type = eventType != null ? eventType : ResolvableType.forInstance(event);
            for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
                invokeListener(listener, event);
            }
        } else {
            super.multicastEvent(event, eventType);
        }
    }
}
