package io.battery.scenario;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.battery.event.CancelledEvent;
import io.battery.event.CancellingEvent;
import io.battery.event.CompletedEvent;
import io.battery.event.StartedEvent;
import io.battery.model.After;
import io.battery.model.BatteryModel;
import io.battery.model.Before;
import io.battery.model.Phase;
import io.battery.model.Scenario;
import io.battery.scenario.run.Run;
import io.battery.scenario.run.RunRecorder;
import io.battery.scenario.run.RunSummary.Outcome;
import io.battery.scenario.step.StepRunner;
import io.battery.scenario.worker.WorkSimulator;
import io.battery.scenario.worker.Worker;

/**
 * Support bean launching a named or random scenario: runs the before steps, then the
 * phases spawning virtual user workers to execute the scenario steps, then the after
 * steps, publishing lifecycle events along the way. Only the most recently launched
 * scenario can be cancelled.
 *
 * @see WorkSimulator
 */
@Component
public class ScenarioLauncher {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private BatteryModel batteryModel;

    @Autowired
    private PhaseRunner phaseRunner;

    @Autowired
    private StepRunner stepRunner;

    @Autowired
    private WorkSimulator workSimulator;

    @Autowired
    private ConnectionPoolSizer connectionPoolSizer;

    @Autowired
    private RunRecorder runRecorder;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private final AtomicReference<CancellationMarker> activeCancellation = new AtomicReference<>();

    public void launchNowAndWait(ScenarioRequest request, CancellationMarker cancellationMarker) {
        launchNow(request, cancellationMarker).join();
    }

    public CompletableFuture<?> launchNow(ScenarioRequest request, CancellationMarker cancellationMarker) {
        final Instant launchTime = Instant.now();

        activeCancellation.set(cancellationMarker);

        final Scenario scenario = !StringUtils.hasLength(request.getName())
                ? batteryModel.findRandomScenario()
                .orElseThrow(() -> new IllegalArgumentException("No random scenario found!"))
                : batteryModel.findNamedScenario(request.getName())
                .orElseThrow(() -> new IllegalArgumentException("No such scenario: " + request.getName()));

        final Run run = runRecorder.start(scenario);

        applicationEventPublisher.publishEvent(new StartedEvent(this, scenario, run.getId()));

        return CompletableFuture.runAsync(() -> launch(scenario, request, cancellationMarker, run))
                .handle((unused, throwable) -> {
                    if (cancellationMarker.check()) {
                        applicationEventPublisher.publishEvent(
                                new CancelledEvent(this,
                                        Duration.between(launchTime, Instant.now()), cancellationMarker));
                    } else {
                        applicationEventPublisher.publishEvent(
                                new CompletedEvent(this,
                                        Duration.between(launchTime, Instant.now()), throwable));
                    }
                    // The run finishes once its VUs complete, which may be later, and may
                    // still be cancelled until then
                    runRecorder.finish(run, () -> cancellationMarker.check()
                            ? new RunRecorder.Ending(Outcome.CANCELLED, cancellationMarker.getReason())
                            : throwable != null
                            ? new RunRecorder.Ending(Outcome.FAILED, describe(throwable))
                            : new RunRecorder.Ending(Outcome.COMPLETED, null));
                    return unused;
                });
    }

    public void cancelActiveScenario(String reason) {
        getCancellationMarker().ifPresentOrElse(m -> {
            m.markCancelled(reason);
            applicationEventPublisher.publishEvent(new CancellingEvent(this, m));
        }, () -> {
            logger.warn("No cancellation marker found");
        });
    }

    private Optional<CancellationMarker> getCancellationMarker() {
        return Optional.ofNullable(activeCancellation.get());
    }

    private static String describe(Throwable throwable) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(throwable);
        return "%s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage());
    }

    private void launch(Scenario scenario, ScenarioRequest request, CancellationMarker cancellationMarker,
                        Run run) {
        final Map<String, Object> compositeState = new HashMap<>();

        // Run any before steps and collect composite state
        final Before before = batteryModel.getBefore();
        if (!request.isSkipBeforeSteps() && !before.getSteps().isEmpty()) {
            Map<String, Object> postState = stepRunner.runSteps(
                    before.getSteps(), compositeState, cancellationMarker);
            compositeState.putAll(postState);
        }

        List<CompletableFuture<Worker>> workFutures = Collections.synchronizedList(new ArrayList<>());

        if (request.isSkipAllPhases()) {
            Phase phase = new Phase();
            phase.setName("Single");
            phase.setUsers(1);

            // The single user's phase lasts until the run finishes, so it's not completed here
            run.phaseStarted(phase);
            workFutures.add(run.addWorker(workSimulator.simulateWork(scenario, phase,
                    Collections.unmodifiableMap(compositeState), cancellationMarker, run)));
        } else {
            // Run each phase and spawn off a unit of work for each virtual user (vu)
            List<Phase> phases = batteryModel.getPhases(
                    phase -> !request.getSkipPhases().contains(phase.getName()));

            final WorkerAdmission workerAdmission = new WorkerAdmission();

            // Size the pool for the phases until all their workers complete, which may be
            // after this launch returns unless the after steps await completion
            final ConnectionPoolSizer.Lease poolLease = connectionPoolSizer.resizeFor(phases);

            try {
                phaseRunner.runPhases(phases, (phase, permits) ->
                        workerAdmission.admit(phase, () -> run.addWorker(workSimulator.simulateWork(scenario, phase,
                                        Collections.unmodifiableMap(compositeState), cancellationMarker, run)))
                                .ifPresent(future -> {
                                    logger.debug("Ramping phase [%s] adding worker %d (%d active)"
                                            .formatted(phase.getName(), permits,
                                                    workerAdmission.getActiveWorkers()));
                                    workFutures.add(future);
                                }), cancellationMarker, run::phaseStarted);
            } finally {
                run.phasesCompleted();
                run.usersDropped(workerAdmission.getDroppedWorkers());
                workerAdmission.logDroppedWorkers();
                CompletableFuture.allOf(workFutures.toArray(new CompletableFuture[] {}))
                        .whenComplete((unused, throwable) -> poolLease.close());
            }
        }

        // Wait for all phased VU workers to run to completion, cancel or error out
        final After after = batteryModel.getAfter();
        if (after.isAwaitCompletion()) {
            CompletableFuture.allOf(workFutures.toArray(new CompletableFuture[] {})).join();
        }

        // Run any after steps on success
        if (!request.isSkipAfterSteps() && !after.getSteps().isEmpty()) {
            stepRunner.runSteps(after.getSteps(), compositeState, cancellationMarker);
        }
    }
}
