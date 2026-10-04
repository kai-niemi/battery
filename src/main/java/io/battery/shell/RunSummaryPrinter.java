package io.battery.shell;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.battery.scenario.run.RunFormat;
import io.battery.scenario.run.RunSummary;
import io.battery.scenario.run.RunSummary.ErrorSummary;
import io.battery.scenario.run.RunSummary.Finding;
import io.battery.scenario.run.RunSummary.PhaseSummary;
import io.battery.shell.support.ListTableModel;
import io.battery.shell.support.ShellSupport;
import io.battery.util.AnsiPrintWriter;

/**
 * Prints a run summary for the shell: the outcome, a table of phases, the totals, latency,
 * users and connection pool, the findings and the most frequent errors.
 */
abstract class RunSummaryPrinter {
    private static final DateTimeFormatter TIME
            = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final int MAX_ERRORS = 5;

    private RunSummaryPrinter() {
    }

    static void print(RunSummary run, AnsiPrintWriter pw) {
        String outcomeColor = switch (run.outcome()) {
            case COMPLETED -> "bg";
            case CANCELLED -> "by";
            case FAILED -> "br";
        };
        pw.println("Run #%d #(bw)%s#(d) #(%s)%s#(d) in %s (%s to %s)".formatted(
                run.id(), run.scenario(), outcomeColor, run.outcome(), RunFormat.millis(run.durationMillis()),
                TIME.format(run.startTime()), TIME.format(run.endTime())));
        if (run.outcomeReason() != null) {
            pw.println("  " + run.outcomeReason());
        }

        if (!run.phases().isEmpty()) {
            pw.println(ShellSupport.prettyPrint(new ListTableModel<>(run.phases(),
                    List.of("Phase", "Time", "Users", "Dropped", "Peak users", "ops/s", "p50", "p99",
                            "Errors", "Waiting"),
                    RunSummaryPrinter::phaseColumn)));
        }

        RunSummary.Totals totals = run.totals();
        pw.println("#(bw)Total#(d)    %s iterations: %s ok, %s transient errors, %s errors · %s ops/s, %s peak"
                .formatted(RunFormat.count(totals.iterations()), RunFormat.count(totals.succeeded()),
                        RunFormat.count(totals.transientErrors()), RunFormat.count(totals.errors()),
                        RunFormat.rate(totals.opsPerSecond()), RunFormat.rate(totals.peakOpsPerSecond())));

        RunSummary.Latency latency = run.latency();
        pw.println("#(bw)Latency#(d)  p50 %s · p90 %s · p99 %s · p99.9 %s · max %s".formatted(
                RunFormat.millis(latency.p50()), RunFormat.millis(latency.p90()), RunFormat.millis(latency.p99()),
                RunFormat.millis(latency.p999()), RunFormat.millis(latency.maxMillis())));

        RunSummary.Workers workers = run.workers();
        pw.println("#(bw)Users#(d)    %d started · %d completed · %d failed · %d cancelled · %d peak concurrent"
                .formatted(workers.started(), workers.completed(), workers.failed(), workers.cancelled(),
                        workers.peakConcurrent()));

        RunSummary.Pool pool = run.pool();
        if (pool != null) {
            pw.println(("#(bw)Pool#(d)     %d connections · %d peak active · %d peak waiting · %d timeouts "
                        + "· acquire %s mean, %s peak").formatted(pool.maxSize(), pool.peakActive(),
                    pool.peakPending(), pool.timeouts(), RunFormat.millis(pool.meanAcquireMillis()),
                    RunFormat.millis(pool.peakAcquireMillis())));
        }

        if (!run.findings().isEmpty()) {
            pw.println("");
            pw.println("#(bw)Findings#(d)");
            run.findings().forEach(finding -> pw.println(" " + format(finding)));
        }

        if (!run.errors().isEmpty()) {
            pw.println("");
            pw.println("#(bw)Errors#(d)");
            run.errors().stream().limit(MAX_ERRORS).forEach(error -> pw.println(" " + format(error)));
            if (run.errors().size() > MAX_ERRORS) {
                pw.println(" ... and %d more".formatted(run.errors().size() - MAX_ERRORS));
            }
        }
        pw.flush();
    }

    private static Object phaseColumn(PhaseSummary phase, int column) {
        return switch (column) {
            case 0 -> phase.name();
            case 1 -> RunFormat.millis(phase.durationMillis());
            case 2 -> phase.usersStarted();
            case 3 -> phase.usersDropped();
            case 4 -> phase.peakUsers();
            case 5 -> RunFormat.rate(phase.totals().opsPerSecond());
            case 6 -> phase.latency().count() > 0 ? RunFormat.millis(phase.latency().p50()) : "-";
            case 7 -> phase.latency().count() > 0 ? RunFormat.millis(phase.latency().p99()) : "-";
            case 8 -> RunFormat.percent(phase.totals().errorRate());
            case 9 -> phase.peakPending();
            default -> "";
        };
    }

    private static String format(Finding finding) {
        String marker = switch (finding.severity()) {
            case ERROR -> "#(br)✖#(d)";
            case WARNING -> "#(by)!#(d)";
            case INFO -> "·";
        };
        return finding.phase() != null
                ? "%s #(bw)%s:#(d) %s".formatted(marker, finding.phase(), finding.message())
                : "%s %s".formatted(marker, finding.message());
    }

    private static String format(ErrorSummary error) {
        List<String> parts = new ArrayList<>();
        parts.add(RunFormat.count(error.count()) + "×");
        parts.add(RunFormat.error(error));
        parts.add(error.transientError() ? "(transient)" : "(ended users)");
        if (error.message() != null) {
            parts.add(error.message().lines().findFirst().orElse(""));
        }
        return String.join(" ", parts);
    }
}
