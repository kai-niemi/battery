package io.battery.shell;

import java.io.StringWriter;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.model.Phase;
import io.battery.scenario.run.Run;
import io.battery.scenario.run.RunFindings;
import io.battery.scenario.run.RunSummary;
import io.battery.scenario.worker.IterationObserver.Outcome;
import io.battery.util.AnsiPrintWriter;

@Tag("unit-test")
public class RunSummaryPrinterTest {
    @Test
    public void givenSummary_expectPrintedSections() {
        Phase phase = new Phase();
        phase.setName("Warm up phase");
        phase.setDuration(Duration.ofSeconds(10));
        phase.setUsers(1);

        Run run = new Run(7, "Insert tokens");
        run.phaseStarted(phase);
        for (int i = 0; i < 50; i++) {
            run.onIteration(Duration.ofMillis(4), Outcome.SUCCESS, null);
        }
        run.onIteration(Duration.ofMillis(9), Outcome.TRANSIENT_ERROR, new SQLException("retry", "40001"));
        run.phasesCompleted();

        RunSummary summary = run.toSummary(Instant.now(), RunSummary.Outcome.COMPLETED, null, List.of());
        summary = summary.withFindings(RunFindings.analyze(summary));

        StringWriter out = new StringWriter();
        RunSummaryPrinter.print(summary, AnsiPrintWriter.wrap(out));

        Assertions.assertThat(out.toString())
                .contains("Run #7", "Insert tokens", "COMPLETED", "Warm up phase",
                        "51 iterations: 50 ok, 1 transient errors, 0 errors",
                        "Findings", "Only 51 iterations",
                        "Errors", "1× SQLException [40001] (transient) retry");
    }
}
