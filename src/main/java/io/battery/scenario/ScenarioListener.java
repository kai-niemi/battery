package io.battery.scenario;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.battery.event.CancelledEvent;
import io.battery.event.CancellingEvent;
import io.battery.event.CompletedEvent;
import io.battery.event.RunFinishedEvent;
import io.battery.event.StartedEvent;
import io.battery.metrics.Problem;
import io.battery.model.Scenario;
import io.battery.scenario.run.RunSummary;

/**
 * Event listener tracking the active scenario, its lifecycle status and last error
 * from launch lifecycle events. Only one scenario is tracked at a time.
 * <p>
 * A scenario stays active from its start until its run finishes, once all its virtual users
 * have completed. That may be well after the launch completed, unless the after steps await
 * completion, so the scenario can still be cancelled meanwhile.
 * <p>
 * Handlers run first among lifecycle listeners, so the status is updated before
 * other listeners (such as page refresh notifications) observe it.
 */
@Component
public class ScenarioListener {
    private record ActiveRun(Scenario scenario, int runId) {
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicReference<ActiveRun> activeRun = new AtomicReference<>();

    private final AtomicReference<ScenarioStatus> activeStatus = new AtomicReference<>(ScenarioStatus.IDLE);

    private final AtomicReference<Throwable> lastError = new AtomicReference<>();

    public Optional<Scenario> getActiveScenario() {
        return Optional.ofNullable(activeRun.get()).map(ActiveRun::scenario);
    }

    public Optional<Integer> getActiveRunId() {
        return Optional.ofNullable(activeRun.get()).map(ActiveRun::runId);
    }

    public Optional<Problem> getLastError() {
        if (lastError.get() != null) {
            return Optional.of(Problem.of(lastError.get()));
        }
        return Optional.empty();
    }

    public ScenarioStatus getActiveStatus() {
        return activeStatus.get();
    }

    public void resetToIdle() {
        activeStatus.set(ScenarioStatus.IDLE);
        lastError.set(null);
        activeRun.set(null);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handle(StartedEvent event) {
        logger.info("Started scenario '%s' as run #%d".formatted(event.getScenario().getName(), event.getRunId()));
        activeRun.set(new ActiveRun(event.getScenario(), event.getRunId()));
        activeStatus.set(ScenarioStatus.RUNNING);
        lastError.set(null);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handle(CompletedEvent event) {
        // The launch completed, but its users may still be running until the run finishes
        ActiveRun active = activeRun.get();
        if (active == null) {
            logger.warn("Ignoring completion with no active scenario", event.getThrowable());
            return;
        }
        if (event.getThrowable() != null) {
            logger.error("Failed scenario '%s'".formatted(active.scenario().getName()), event.getThrowable());
            lastError.set(event.getThrowable());
        } else {
            logger.debug("Launched scenario '%s', finishing once its users complete"
                    .formatted(active.scenario().getName()));
        }
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handle(CancellingEvent event) {
        // Published on the cancelling thread, so it may race with the run finishing.
        // Only a running scenario can transition to cancelling.
        ActiveRun active = activeRun.get();
        if (active == null || !activeStatus.compareAndSet(ScenarioStatus.RUNNING, ScenarioStatus.CANCELLING)) {
            logger.debug("Ignoring cancellation with no running scenario - %s"
                    .formatted(event.getCancellationMarker().getReason()));
            return;
        }
        logger.warn("Cancelling scenario '%s' - %s"
                .formatted(active.scenario().getName(), event.getCancellationMarker().getReason()));
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handle(CancelledEvent event) {
        // The launch stopped, but its users may still be stopping until the run finishes
        logger.debug("Cancelled scenario launch - %s".formatted(event.getCancellationMarker().getReason()));
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handle(RunFinishedEvent event) {
        RunSummary summary = event.getSummary();

        // Claim the active run atomically, ignoring runs no longer tracked (such as after a reset)
        ActiveRun active = activeRun.get();
        if (active == null || active.runId() != summary.id() || !activeRun.compareAndSet(active, null)) {
            logger.debug("Ignoring finished run #%d that isn't the active one".formatted(summary.id()));
            return;
        }

        switch (summary.outcome()) {
            case COMPLETED -> {
                logger.info("Completed scenario '%s'".formatted(active.scenario().getName()));
                activeStatus.set(ScenarioStatus.COMPLETED);
                lastError.set(null);
            }
            case CANCELLED -> {
                logger.info("Cancelled scenario '%s' - %s"
                        .formatted(active.scenario().getName(), summary.outcomeReason()));
                activeStatus.set(ScenarioStatus.CANCELLED);
                lastError.set(null);
            }
            // The error was logged and kept when the launch failed
            case FAILED -> activeStatus.set(ScenarioStatus.FAILED);
        }
    }
}
