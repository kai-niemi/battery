package io.battery.scenario;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.event.CancelledEvent;
import io.battery.event.CancellingEvent;
import io.battery.event.CompletedEvent;
import io.battery.event.RunFinishedEvent;
import io.battery.event.StartedEvent;
import io.battery.model.Scenario;
import io.battery.scenario.run.RunSummaries;
import io.battery.scenario.run.RunSummary.Outcome;

@Tag("unit-test")
public class ScenarioListenerTest {
    private ScenarioListener listener;

    private CancellationMarker cancellationMarker;

    @BeforeEach
    public void setup() {
        listener = new ScenarioListener();
        cancellationMarker = new CancellationMarker();
        cancellationMarker.markCancelled("test");
    }

    private void start(int runId) {
        Scenario scenario = new Scenario();
        scenario.setName("test");
        listener.handle(new StartedEvent(this, scenario, runId));
    }

    private void finish(int runId, Outcome outcome) {
        listener.handle(new RunFinishedEvent(this, RunSummaries.outcome(runId, outcome, "test")));
    }

    @Test
    public void givenRunningScenario_whenCancelled_expectCancelledOnceRunFinishes() {
        start(1);
        listener.handle(new CancellingEvent(this, cancellationMarker));
        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.CANCELLING);

        // Users may still be stopping once the launch is cancelled
        listener.handle(new CancelledEvent(this, Duration.ZERO, cancellationMarker));
        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.CANCELLING);
        Assertions.assertThat(listener.getActiveScenario()).isPresent();

        finish(1, Outcome.CANCELLED);
        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.CANCELLED);
        Assertions.assertThat(listener.getActiveScenario()).isEmpty();
    }

    @Test
    public void givenCompletedLaunch_whenUsersStillRunning_expectActiveAndCancellable() {
        start(1);
        listener.handle(new CompletedEvent(this, Duration.ZERO, null));

        // Previously no longer active, so it couldn't be cancelled while its users ran
        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.RUNNING);
        Assertions.assertThat(listener.getActiveScenario()).isPresent();

        listener.handle(new CancellingEvent(this, cancellationMarker));
        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.CANCELLING);

        finish(1, Outcome.CANCELLED);
        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.CANCELLED);
        Assertions.assertThat(listener.getActiveScenario()).isEmpty();
    }

    @Test
    public void givenCompletedLaunch_whenRunFinished_expectCompleted() {
        start(1);
        listener.handle(new CompletedEvent(this, Duration.ZERO, null));
        finish(1, Outcome.COMPLETED);

        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.COMPLETED);
        Assertions.assertThat(listener.getActiveScenario()).isEmpty();
    }

    @Test
    public void givenFinishedRun_whenCancelling_expectIgnored() {
        start(1);
        listener.handle(new CompletedEvent(this, Duration.ZERO, null));
        finish(1, Outcome.COMPLETED);

        // Cancellation racing with the run finishing
        listener.handle(new CancellingEvent(this, cancellationMarker));
        listener.handle(new CancelledEvent(this, Duration.ZERO, cancellationMarker));

        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.COMPLETED);
        Assertions.assertThat(listener.getActiveScenario()).isEmpty();
    }

    @Test
    public void givenFailedLaunch_expectFailedOnceRunFinishes() {
        start(1);
        listener.handle(new CompletedEvent(this, Duration.ZERO, new IllegalStateException("test")));
        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.RUNNING);
        Assertions.assertThat(listener.getLastError()).isPresent();

        finish(1, Outcome.FAILED);
        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.FAILED);
        Assertions.assertThat(listener.getLastError()).isPresent();
    }

    @Test
    public void givenCancellingScenario_whenLaunchFailed_expectFailedOnceRunFinishes() {
        start(1);
        listener.handle(new CancellingEvent(this, cancellationMarker));
        listener.handle(new CompletedEvent(this, Duration.ZERO, new IllegalStateException("test")));
        finish(1, Outcome.FAILED);

        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.FAILED);
        Assertions.assertThat(listener.getLastError()).isPresent();
    }

    @Test
    public void givenResetDuringRun_whenRunFinished_expectIgnored() {
        start(1);
        listener.resetToIdle();
        listener.handle(new CompletedEvent(this, Duration.ZERO, new IllegalStateException("test")));
        finish(1, Outcome.FAILED);

        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.IDLE);
        Assertions.assertThat(listener.getLastError()).isEmpty();
    }

    @Test
    public void givenNewRunAfterReset_whenPreviousRunFinishes_expectNewRunStillActive() {
        start(1);
        listener.resetToIdle();
        start(2);

        finish(1, Outcome.COMPLETED);

        Assertions.assertThat(listener.getActiveStatus()).isEqualTo(ScenarioStatus.RUNNING);
        Assertions.assertThat(listener.getActiveScenario()).isPresent();
    }
}
