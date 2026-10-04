package io.battery.scenario;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.battery.model.Phase;

/**
 * Admission control for virtual user workers, capping the number of concurrently
 * active workers to the phase max concurrency. Workers are counted as active from
 * admission until their future completes, including workers admitted in earlier phases.
 * <p>
 * Arrivals beyond the cap are dropped rather than deferred to keep the phase arrival
 * rate and duration intact. Dropped arrivals are counted per phase.
 * <p>
 * Not thread-safe except for the active worker count: admissions and dropped worker
 * access are confined to the phase runner thread, while workers complete on other threads.
 */
class WorkerAdmission {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // Decremented by worker completion on other threads
    private final AtomicInteger activeWorkers = new AtomicInteger();

    private final Map<String, Integer> droppedWorkers = new LinkedHashMap<>();

    /**
     * Admits a new worker unless the phase max concurrency is reached.
     *
     * @param phase   the phase with an optional max concurrency (0 for unlimited)
     * @param spawner the worker spawner invoked if admitted
     * @param <T>     the worker future type
     * @return the worker future, or empty if dropped
     */
    public <T> Optional<CompletableFuture<T>> admit(Phase phase, Supplier<CompletableFuture<T>> spawner) {
        final int maxConcurrency = phase.getMaxConcurrency();

        // Reserve a slot atomically with respect to concurrent worker completions
        int active = activeWorkers.getAndUpdate(n -> maxConcurrency <= 0 || n < maxConcurrency ? n + 1 : n);
        if (maxConcurrency > 0 && active >= maxConcurrency) {
            int dropped = droppedWorkers.merge(phase.getName(), 1, Integer::sum);
            if (dropped == 1) {
                logger.warn("Phase [%s] reached max concurrency %d, dropping new users until workers complete"
                        .formatted(phase.getName(), maxConcurrency));
            }
            return Optional.empty();
        }

        try {
            CompletableFuture<T> future = spawner.get();
            future.whenComplete((unused, throwable) -> activeWorkers.decrementAndGet());
            return Optional.of(future);
        } catch (RuntimeException e) {
            activeWorkers.decrementAndGet();
            throw e;
        }
    }

    public int getActiveWorkers() {
        return activeWorkers.get();
    }

    /**
     * @return the number of dropped users keyed by phase name, in phase order
     */
    public Map<String, Integer> getDroppedWorkers() {
        return Collections.unmodifiableMap(droppedWorkers);
    }

    public void logDroppedWorkers() {
        droppedWorkers.forEach((phase, dropped) ->
                logger.warn("Phase [%s] dropped %d users at max concurrency".formatted(phase, dropped)));
    }
}
