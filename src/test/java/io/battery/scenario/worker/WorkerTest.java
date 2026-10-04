package io.battery.scenario.worker;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.metrics.Problem;

@Tag("unit-test")
public class WorkerTest {
    private static Worker worker() {
        return new Worker(1, "scenario", "phase", Duration.ofMinutes(1));
    }

    @Test
    public void givenRunningWorker_expectNoCompletionDuration() {
        Worker worker = worker();
        Assertions.assertThat(worker.isRunning()).isTrue();
        Assertions.assertThat(worker.getCompletionDuration()).isZero();
        Assertions.assertThat(worker.getCompletionTime()).isEqualTo("-");
    }

    @Test
    public void givenEndedWorker_expectFixedCompletionDuration() throws InterruptedException {
        Worker worker = worker();
        TimeUnit.MILLISECONDS.sleep(20);
        worker.markCompleted();

        Duration completion = worker.getCompletionDuration();
        TimeUnit.MILLISECONDS.sleep(50);

        // The completion duration must not keep growing after the worker ended
        Assertions.assertThat(completion).isGreaterThanOrEqualTo(Duration.ofMillis(20));
        Assertions.assertThat(worker.getCompletionDuration()).isEqualTo(completion);
        Assertions.assertThat(worker.isCompleted()).isTrue();
    }

    @Test
    public void givenProblems_expectLastProblem() {
        Worker worker = worker();
        Assertions.assertThat(worker.getLastProblem()).isNull();

        worker.addProblem(Problem.of(new IllegalStateException("first")));
        worker.addProblem(Problem.of(new IllegalStateException("second")));

        Assertions.assertThat(worker.getProblems()).hasSize(2);
        Assertions.assertThat(worker.getLastProblem()).isEqualTo(worker.getProblems().get(1));

        worker.clearProblem();
        Assertions.assertThat(worker.getLastProblem()).isNull();
    }
}
