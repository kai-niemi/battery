package io.battery.scenario.worker;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import io.battery.metrics.Metrics;
import io.battery.metrics.Problem;
import io.battery.model.Phase;
import io.battery.model.Scenario;
import io.battery.model.ScenarioExecutionException;
import io.battery.scenario.CancellationMarker;
import io.battery.scenario.SQLExceptionClassifier;
import io.battery.scenario.ScenarioCancellationException;
import io.battery.scenario.step.StepRunner;

/**
 * Support bean starting workers, each representing a virtual user that repeatedly runs
 * the steps of a scenario until the scenario duration elapses or the launch is cancelled.
 * <p>
 * Transactional scenarios run all steps of an iteration in one explicit transaction.
 * Transient SQL errors are retried, with optional backoff, if the scenario allows it,
 * while other errors fail the worker. Workers run concurrently on virtual threads using
 * an {@link AsyncTaskExecutor} and are registered with the {@link WorkTracker} for
 * instrumentation.
 *
 * @see WorkTracker
 */
@Component
public class WorkSimulator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicInteger monotonicId = new AtomicInteger();

    @Autowired
    private WorkTracker workTracker;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    @Qualifier("asyncTaskExecutor")
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private StepRunner stepRunner;

    /**
     * @param iterationObserver observer of each iteration, called on the worker's thread
     */
    public CompletableFuture<Worker> simulateWork(Scenario scenario,
                                                  Phase phase,
                                                  Map<String, Object> initialState,
                                                  CancellationMarker cancellationMarker,
                                                  IterationObserver iterationObserver) {
        // Wrap step runner in a unit-of-work for metrics and visibility
        final Worker worker = new Worker(monotonicId.incrementAndGet(),
                scenario.getName(), phase.getName(), scenario.getDuration());
        workTracker.addWorker(worker);

        // Let it rip!
        return CompletableFuture.supplyAsync(() -> {
                    // Time to quit in future
                    Instant futureTime = Instant.now().plus(scenario.getDuration());

                    // Compose completion predicate checked in each iteration cycle
                    Predicate<Integer> completionPredicate
                            = x -> Instant.now().isBefore(futureTime) && !cancellationMarker.check();

                    final Map<String, Object> compositeState = new HashMap<>(initialState);

                    repeatUntil(scenario, worker, completionPredicate, iterationObserver, (iteration) -> {
                        Map<String, Object> postState;

                        if (scenario.isTransactional()) {
                            // Wrap all steps in one explicit transaction
                            postState = transactionTemplate.execute(transactionStatus ->
                                    stepRunner.runSteps(scenario.getSteps(iteration),
                                            compositeState, cancellationMarker));
                        } else {
                            // Run all steps in individual implicit transactions
                            postState = stepRunner.runSteps(scenario.getSteps(iteration),
                                            compositeState, cancellationMarker);
                        }
                        // Merge the post-state into the composite state since steps may have modified
                        // the state and only iterate once.
                        compositeState.putAll(postState);
                    });

                    return worker;
                }, asyncTaskExecutor)
                .handle((w, throwable) -> {
                    if (throwable != null) {
                        worker.markFailed();
                        worker.addProblem(Problem.of(throwable));
                    } else {
                        if (cancellationMarker.check()) {
                            worker.markCancelled();
                        } else {
                            worker.markCompleted();
                        }
                    }
                    return worker;
                });
    }

    private void repeatUntil(Scenario scenario,
                             Worker worker,
                             Predicate<Integer> predicate,
                             IterationObserver iterationObserver,
                             UnitOfWork unitOfWork) {
        int retries = 0;
        int iterations = 0;

        Metrics metrics = worker.getMetrics();

        while (predicate.test(iterations++)) {
            final Instant callTime = Instant.now();

            try {
                unitOfWork.call(iterations);

                retries = 0;
                Duration duration = Duration.between(callTime, Instant.now());
                metrics.markSuccess(duration);

                iterationObserver.onIteration(duration, IterationObserver.Outcome.SUCCESS, null);
            } catch (Exception ex) {
                if (ex instanceof ScenarioCancellationException) {
                    // Canceled between steps of this iteration, which isn't an error
                    return;
                }

                Duration duration = Duration.between(callTime, Instant.now());
                Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);

                if (isTransient(cause) && scenario.isContinueOnTransientErrors()) {
                    metrics.markFail(duration, true);
                    iterationObserver.onIteration(duration, IterationObserver.Outcome.TRANSIENT_ERROR, ex);

                    // Record the problem
                    worker.addProblem(Problem.of(ex));

                    if (scenario.isBackoffOnTransientErrors()) {
                        backoffDelayWithJitter(++retries);
                    }
                } else {
                    metrics.markFail(duration, false);
                    iterationObserver.onIteration(duration, IterationObserver.Outcome.ERROR, ex);

                    throw new ScenarioExecutionException(scenario, ex);
                }
            }
        }
    }

    private final SQLExceptionClassifier exceptionClassifier = new SQLExceptionClassifier() {
    };

    /**
     * @return true if the exception is transient and recoverable
     */
    boolean isTransient(Throwable cause) {
        if (cause instanceof SQLTransientConnectionException) {
            // Typically a connection pool timeout, which carries no SQL state unless creating
            // a connection failed. No statement was executed, so it's always safe to retry.
            logger.warn("Transient connection exception: [%s]".formatted(cause));
            return true;
        } else if (cause instanceof SQLException) {
            String sqlState = ((SQLException) cause).getSQLState();
            if (exceptionClassifier.isTransient((SQLException) cause)) {
                logger.warn("Transient SQL exception [%s]: [%s]".formatted(sqlState, cause));
                return true;
            } else {
                return false;
            }
        } else if (cause instanceof TransientDataAccessException) {
            logger.warn("Transient data access exception: [%s]".formatted(cause));
            return true;
        }
        return false;
    }

    private void backoffDelayWithJitter(int inc) {
        try {
            TimeUnit.MILLISECONDS.sleep(Math.min((long) (Math.pow(2, inc) + Math.random() * 1000), 5000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
