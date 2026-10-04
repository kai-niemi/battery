package io.battery.shell;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.jline.tui.table.BeanListTableModel;

import io.battery.metrics.Problem;
import io.battery.scenario.worker.WorkTracker;
import io.battery.scenario.worker.Worker;
import io.battery.shell.support.ShellSupport;
import io.battery.web.api.WorkerResourceAssembler;
import static io.battery.util.AnsiPrintWriter.wrap;

@ShellComponent
public class WorkerCommands extends AbstractShellCommand {
    @Autowired
    private WorkTracker workTracker;

    @Autowired
    private WorkerResourceAssembler workerResourceAssembler;

    @Autowired
    private ShellSupport shellSupport;

    @Command(value = "Clear non-running virtual user workers",
            name = {"clear", "workers"},
            alias = {"cw"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.SCENARIO_COMMANDS)
    public void clearWorkers() {
        workTracker.deleteAll();
    }

    @Command(value = "Clear virtual user worker errors",
            name = {"clear", "errors"},
            alias = {"ce"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.SCENARIO_COMMANDS)
    public void clearWorkerErrors() {
        workTracker.listWorkers()
                .forEach(Worker::clearProblem);
    }

    @Command(value = "Show virtual user worker problems",
            name = {"show", "errors"},
            alias = {"se"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.SCENARIO_COMMANDS)
    public void showProblems(@Option(description = "problem limit", defaultValue = "15") int limit,
                             @Option(description = "filter by worker id", defaultValue = "-1") int id,
                             CommandContext ctx) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("className", "Type");
        header.put("message", "Message");
        header.put("createdAt", "Created");
        header.put("stackTrace", "Cause");

        PrintWriter pw = wrap(ctx.outputWriter());

        workTracker.listWorkers().stream()
                .filter(worker -> id <= 0 || worker.getId() == id)
                .forEach(worker -> {
                    List<Problem> problems = worker.getProblems(limit);
                    if (!problems.isEmpty()) {
                        pw.printf("-- #(bc)%s - %s#(d) -- %n", worker.getScenario(), worker.getPhase());
                        pw.printf("%s%n",
                                ShellSupport.prettyPrint(new BeanListTableModel<>(problems, header)));
                        pw.flush();
                    }
                });
    }

    @Command(value = "Show virtual user workers",
            name = {"show", "workers"},
            alias = {"sw"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.SCENARIO_COMMANDS)
    public void showWorkers(@Option(description = "page size", defaultValue = "20") int pageSize,
                            @Option(description = "enable pagination", defaultValue = "false") boolean pagination,
                            @Option(description = "skip completed", defaultValue = "false") boolean skipCompleted,
                            @Option(description = "toggle interval printing", defaultValue = "false") boolean toggle,
                            @Option(description = "printing interval in seconds", defaultValue = "3", shortName = 'i')
                            int interval,
                            CommandContext ctx) {
        PrintWriter pw = wrap(ctx.outputWriter());
        if (toggle) {
            togglePrintWorkers(pageSize, skipCompleted, interval, pw);
        } else {
            printWorkers(pageSize, pagination, skipCompleted, pw);
        }
    }

    private ScheduledFuture<?> scheduledFuture;

    private void togglePrintWorkers(int pageSize, boolean skipCompleted, int interval, PrintWriter pw) {
        boolean enabled;

        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
            enabled = false;
        } else {
            scheduledFuture = Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(() -> printWorkers(pageSize, false, skipCompleted, pw),
                            interval, interval, TimeUnit.SECONDS);
            enabled = true;
        }

        pw.printf("Periodic worker printing #(bright_yellow)%s#(default) with %ds interval%n"
                .formatted(enabled ? "ENABLED" : "DISABLED", interval));
    }

    private void printWorkers(int pageSize, boolean pagination, boolean skipCompleted, PrintWriter pw) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("id", "ID");
        header.put("scenario", "Scenario");
        header.put("phase", "Phase");
        header.put("remainingTime", "Remaining");
        header.put("opsPerSecond", "op/s");
        header.put("p90", "P90 (ms)");
        header.put("p99", "P99 (ms)");
        header.put("success", "Success");
        header.put("transientErrors", "Transient");
        header.put("nonTransientErrors", "Non-Transient");
        header.put("status", "Status");
        header.put("lastProblem", "Last Problem");

        if (pagination) {
            Pageable page = PageRequest.ofSize(pageSize);
            while (page.isPaged()) {
                Page<Worker> workers =
                        workTracker.listWorkers(page,
                                worker -> !skipCompleted || !worker.isCompleted());

                pw.printf("%s%n",
                        ShellSupport.prettyPrint(new BeanListTableModel<>(
                                workerResourceAssembler.toCollectionModel(workers.getContent()), header)));
                pw.flush();

                page = shellSupport.askForPage(workers).orElseGet(Pageable::unpaged);
            }
        } else {
            List<Worker> workers = workTracker.listWorkers(pageSize,
                    worker -> !skipCompleted || !worker.isCompleted());
            if (!workers.isEmpty()) {
                pw.printf("%s%n",
                        ShellSupport.prettyPrint(new BeanListTableModel<>(
                                workerResourceAssembler.toCollectionModel(workers), header)));
                pw.flush();
            }
        }
    }
}
