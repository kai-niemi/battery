package io.battery.scenario.worker;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.test.util.ReflectionTestUtils;

import io.battery.model.Phase;
import io.battery.model.Scenario;
import io.battery.model.Step;
import io.battery.scenario.CancellationMarker;
import io.battery.scenario.ScenarioCancellationException;
import io.battery.scenario.step.StepRunner;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit-test")
public class WorkSimulatorTest {
    private final WorkSimulator workSimulator = new WorkSimulator();

    @Test
    public void givenPoolTimeoutWithoutSqlState_expectTransient() {
        // As thrown by Hikari when no connection becomes available within the timeout
        Assertions.assertThat(workSimulator.isTransient(new SQLTransientConnectionException(
                        "battery-pool - Connection is not available, request timed out after 5000ms", null, null)))
                .isTrue();
    }

    @Test
    public void givenSerializationFailure_expectTransient() {
        Assertions.assertThat(workSimulator.isTransient(new SQLException("retry", "40001"))).isTrue();
    }

    @Test
    public void givenSyntaxError_expectNotTransient() {
        Assertions.assertThat(workSimulator.isTransient(new SQLException("syntax", "42601"))).isFalse();
    }

    @Test
    public void givenCancellationDuringIteration_expectCancelledWorkerWithoutError() {
        Step step = new Step();
        step.setName("insert");
        step.setSql("select 1");

        Scenario scenario = new Scenario();
        scenario.setName("scenario");
        scenario.setDuration(Duration.ofMinutes(1));
        scenario.setSteps(List.of(step));

        // Cancelled by another thread between the iteration check and the next step
        CancellationMarker cancellationMarker = new CancellationMarker();
        StepRunner stepRunner = mock(StepRunner.class);
        when(stepRunner.runSteps(anyList(), anyMap(), eq(cancellationMarker))).thenAnswer(invocation -> {
            cancellationMarker.markCancelled("test");
            throw new ScenarioCancellationException(step, cancellationMarker);
        });

        ReflectionTestUtils.setField(workSimulator, "workTracker", mock(WorkTracker.class));
        ReflectionTestUtils.setField(workSimulator, "stepRunner", stepRunner);
        ReflectionTestUtils.setField(workSimulator, "asyncTaskExecutor", new TaskExecutorAdapter(Runnable::run));

        List<IterationObserver.Outcome> outcomes = new ArrayList<>();
        Phase phase = new Phase();
        phase.setName("phase");

        Worker worker = workSimulator.simulateWork(scenario, phase, Map.of(), cancellationMarker,
                (duration, outcome, cause) -> outcomes.add(outcome)).join();

        // Previously failed with the cancellation recorded as an error
        Assertions.assertThat(worker.getStatus()).isEqualTo(WorkerStatus.CANCELLED);
        Assertions.assertThat(worker.getProblems()).isEmpty();
        Assertions.assertThat(worker.getMetrics().getNonTransientFail()).isZero();
        Assertions.assertThat(outcomes).isEmpty();
    }
}
