package io.battery.shell;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.availability.AvailabilityProvider;

import io.battery.event.RunFinishedEvent;
import io.battery.scenario.CancellationMarker;
import io.battery.scenario.ScenarioLauncher;
import io.battery.scenario.ScenarioListener;
import io.battery.scenario.ScenarioRequest;
import io.battery.scenario.run.RunFormat;
import io.battery.scenario.run.RunRecorder;
import io.battery.scenario.run.RunSummary;
import io.battery.shell.support.ListTableModel;
import io.battery.shell.support.ShellSupport;
import io.battery.util.AnsiPrintWriter;
import static io.battery.util.AnsiPrintWriter.wrap;

@ShellComponent
public class ScenarioCommands extends AbstractShellCommand {
    @Autowired
    private ScenarioLauncher scenarioLauncher;

    @Autowired
    private ScenarioListener scenarioListener;

    @Autowired
    private RunRecorder runRecorder;

    // Output of the last run command, which run summaries are printed to
    private volatile AnsiPrintWriter runSummaryWriter;

    @EventListener
    public void onRunFinished(RunFinishedEvent event) {
        AnsiPrintWriter pw = runSummaryWriter;
        if (pw != null) {
            pw.println("");
            RunSummaryPrinter.print(event.getSummary(), pw);
        }
    }

    @Bean
    public AvailabilityProvider ifRunning() {
        return () -> scenarioListener.getActiveScenario().isPresent()
                ? Availability.available()
                : Availability.unavailable("no scenario is running!");
    }

    @Bean
    public AvailabilityProvider ifNotRunning() {
        return () -> scenarioListener.getActiveScenario().isEmpty()
                ? Availability.available()
                : Availability.unavailable("a scenario is already running!");
    }

    @Command(value = "Run a named or random scenario",
            name = {"run"},
            alias = {"r"},
            group = CommandGroups.SCENARIO_COMMANDS,
            exitStatusExceptionMapper = "commandExceptionMapper",
            availabilityProvider = "ifNotRunning",
            completionProvider = "scenarioProvider")
    public void runScenario(
            @Option(description = "scenario name or alias, or empty for weighted random", longName = "name")
            String name,
            @Option(description = "skip all before steps", defaultValue = "false") boolean skipBefore,
            @Option(description = "skip all after steps", defaultValue = "false") boolean skipAfter,
            @Option(description = "skip all phases and just start a single virtual user (for testing)",
                    defaultValue = "false") boolean skipPhases,
            CommandContext ctx
    ) {
        // Print the summary once the run, including all its users, has finished
        runSummaryWriter = wrap(ctx.outputWriter());

        scenarioLauncher.launchNow(
                ScenarioRequest.newInstance()
                        .setName(name)
                        .setSkipBeforeSteps(skipBefore)
                        .setSkipAfterSteps(skipAfter)
                        .setSkipAllPhases(skipPhases),
                new CancellationMarker());

        AnsiPrintWriter pw = wrap(ctx.outputWriter());
        pw.println("Use #(by)'cancel'#(d) to cancel the active scenario");
        pw.println("Use #(by)'show workers'#(d) to see VU workers");
        pw.println("Use #(by)'show status'#(d) to see the active scenario");
        pw.println("Use #(by)'show runs'#(d) to see finished runs");
    }

    @Command(value = "Cancel active scenario",
            name = {"cancel"},
            alias = {"c"},
            group = CommandGroups.SCENARIO_COMMANDS,
            exitStatusExceptionMapper = "commandExceptionMapper",
            availabilityProvider = "ifRunning")
    public void cancelScenario(CommandContext ctx) {
        scenarioLauncher.cancelActiveScenario("by shell command");
    }

    @Command(value = "Show recent scenario runs",
            name = {"show", "runs"},
            alias = {"sr"},
            group = CommandGroups.SCENARIO_COMMANDS,
            exitStatusExceptionMapper = "commandExceptionMapper")
    public void showRuns(CommandContext ctx) {
        AnsiPrintWriter pw = wrap(ctx.outputWriter());

        List<RunSummary> runs = runRecorder.getRecentRuns();
        if (runs.isEmpty()) {
            pw.println("No finished runs");
            return;
        }

        pw.println(ShellSupport.prettyPrint(new ListTableModel<>(runs,
                List.of("Run", "Scenario", "Started", "Duration", "Outcome", "Iterations", "ops/s", "p99",
                        "Errors", "Findings"),
                (run, column) -> switch (column) {
                    case 0 -> "#" + run.id();
                    case 1 -> run.scenario();
                    case 2 -> run.startTime().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            .truncatedTo(ChronoUnit.SECONDS);
                    case 3 -> RunFormat.millis(run.durationMillis());
                    case 4 -> run.outcome();
                    case 5 -> RunFormat.count(run.totals().iterations());
                    case 6 -> RunFormat.rate(run.totals().opsPerSecond());
                    case 7 -> RunFormat.millis(run.latency().p99());
                    case 8 -> RunFormat.percent(run.totals().errorRate());
                    case 9 -> run.findings().size();
                    default -> "";
                })));
    }

    @Command(value = "Show the summary of a scenario run",
            name = {"show", "run"},
            alias = {"su"},
            group = CommandGroups.SCENARIO_COMMANDS,
            exitStatusExceptionMapper = "commandExceptionMapper")
    public void showRun(@Option(description = "run number, or the last run if omitted", defaultValue = "-1") int id,
                        CommandContext ctx) {
        AnsiPrintWriter pw = wrap(ctx.outputWriter());
        Optional<RunSummary> run = id < 0 ? runRecorder.getLastRun() : runRecorder.getRun(id);
        run.ifPresentOrElse(summary -> RunSummaryPrinter.print(summary, pw),
                () -> pw.println(id < 0 ? "No finished runs" : "No such recent run: #" + id));
    }

    @Command(value = "Show scenario status",
            name = {"show","status"},
            alias = {"ss"},
            group = CommandGroups.SCENARIO_COMMANDS,
            exitStatusExceptionMapper = "commandExceptionMapper",
            availabilityProvider = "ifRunning")
    public void showStatus(CommandContext ctx) {
        AnsiPrintWriter pw = wrap(ctx.outputWriter());

        scenarioListener.getActiveScenario().ifPresentOrElse(scenario -> {
            pw.printf("Active scenario: #(by)%s#(d)%n", scenario.describe());
            pw.printf("Status: #(by)%s#(d)%n", scenarioListener.getActiveStatus());
        }, () -> {
            pw.printf("No active scenario (%s)%n", scenarioListener.getActiveStatus());
        });
    }
}
