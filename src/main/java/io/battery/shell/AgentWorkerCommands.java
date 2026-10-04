package io.battery.shell;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.jline.tui.table.BeanListTableModel;
import org.springframework.web.client.ResourceAccessException;

import io.battery.metrics.Problem;
import io.battery.model.BatteryModel;
import io.battery.shell.support.ShellSupport;
import io.battery.util.AnsiPrintWriter;
import io.battery.web.HypermediaClient;
import io.battery.web.api.model.MessageModel;
import io.battery.web.api.model.WorkerModel;
import static org.springframework.hateoas.mediatype.hal.HalLinkRelation.curied;
import static io.battery.util.AnsiPrintWriter.wrap;
import static io.battery.web.api.model.LinkRelations.CURIE_NAMESPACE;
import static io.battery.web.api.model.LinkRelations.PROBLEMS_REL;
import static io.battery.web.api.model.LinkRelations.WORKERS_DELETE_REL;
import static io.battery.web.api.model.LinkRelations.WORKERS_REL;
import static io.battery.web.api.model.LinkRelations.WORKER_REL;

@ShellComponent
public class AgentWorkerCommands extends AbstractShellCommand {
    private static final ParameterizedTypeReference<PagedModel<WorkerModel>>
            PAGED_WORKER_MODEL_TYPE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<PagedModel<Problem>>
            PAGED_PROBLEM_TYPE = new ParameterizedTypeReference<>() {
    };

    @Autowired
    private HypermediaClient hypermediaClient;

    @Autowired
    private BatteryModel batteryModel;

    @Command(value = "Clear all non-running virtual user workers on agent(s)",
            name = {"agent", "clear", "workers"},
            alias = {"acw"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            completionProvider = "agentProvider",
            group = CommandGroups.AGENT_COMMANDS)
    public void clearAgentWorkers(@Option(description = "agent name or empty to include all") String name,
                                  CommandContext ctx) {
        batteryModel.getNetwork().findAgents(name)
                .forEach(agent -> {
                    AnsiPrintWriter pw = wrap(ctx.outputWriter());

                    try {
                        ResponseEntity<MessageModel> model = hypermediaClient.from(agent.indexLink())
                                .follow(curied(CURIE_NAMESPACE, WORKER_REL).value())
                                .toEntity(MessageModel.class);

                        ResponseEntity<MessageModel> response = hypermediaClient.delete(
                                model.getBody().getRequiredLink(curied(CURIE_NAMESPACE, WORKERS_DELETE_REL)),
                                MessageModel.class);
                        if (response.getStatusCode().is2xxSuccessful()) {
                            pw.greenln("HTTP status: %s".formatted(response.getStatusCode()));
                        } else {
                            pw.yellowln("Unexpected HTTP status: %s".formatted(response.getStatusCode()));
                        }
                    } catch (ResourceAccessException e) {
                        pw.redln("Agent not available: %s".formatted(e.getMessage()));
                    } finally {
                        pw.flush();
                    }
                });
    }

    @Command(value = "Show virtual user worker errors on agent(s)",
            name = {"agent", "show", "errors"},
            alias = {"ase"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            completionProvider = "agentProvider",
            group = CommandGroups.AGENT_COMMANDS)
    public void showAgentErrors(@Option(description = "problem limit", defaultValue = "15") int limit,
                                @Option(description = "agent name or empty to include all") String name,
                                CommandContext ctx) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("className", "Type");
        header.put("message", "Message");
        header.put("createdAt", "Created");
        header.put("stacktrace", "Cause");

        batteryModel.getNetwork().findAgents(name)
                .forEach(agent -> {
                    AnsiPrintWriter pw = wrap(ctx.outputWriter());

                    try {
                        PagedModel<Problem> pagedModel = hypermediaClient.from(agent.indexLink())
                                .follow(curied(CURIE_NAMESPACE, WORKERS_REL).value())
                                .follow(curied(CURIE_NAMESPACE, PROBLEMS_REL).value())
                                .withTemplateParameters(Map.of("page", 0, "size", limit))
                                .toObject(PAGED_PROBLEM_TYPE);

                        if(!Objects.requireNonNull(pagedModel).getContent().isEmpty()) {
                            pw.printf("-- #(bc)%s (%s)#(d) -- %n", agent.getName(), agent.getUrl());
                            pw.printf("%s%n",
                                    ShellSupport.prettyPrint(new BeanListTableModel<>(
                                            pagedModel.getContent(), header)));
                        }
                    } catch (ResourceAccessException e) {
                        pw.redln("Agent not available: %s".formatted(e.getMessage()));
                    } finally {
                        pw.flush();
                    }
                });
    }

    @Command(value = "Show virtual user workers on agent(s)",
            name = {"agent", "show", "workers"},
            alias = {"asw"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            completionProvider = "agentProvider",
            group = CommandGroups.AGENT_COMMANDS)
    public void showAgentWorkers(@Option(description = "page size", defaultValue = "20") int pageSize,
                                 @Option(description = "agent name or empty to include all") String name,
                                 CommandContext ctx) {

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

        batteryModel.getNetwork().findAgents(name)
                .forEach(agent -> {
                    AnsiPrintWriter pw = wrap(ctx.outputWriter());

                    try {
                        PagedModel<WorkerModel> pagedModel = hypermediaClient.from(agent.indexLink())
                                .follow(curied(CURIE_NAMESPACE, WORKERS_REL).value())
                                .withTemplateParameters(Map.of("page", 0, "size", pageSize))
                                .toObject(PAGED_WORKER_MODEL_TYPE);

                        pw.printf("-- #(bc)%s (%s)#(d) -- %n", agent.getName(), agent.getUrl());
                        pw.printf("%s%n",
                                ShellSupport.prettyPrint(new BeanListTableModel<>(
                                        pagedModel.getContent(), header)));
                    } catch (ResourceAccessException e) {
                        pw.redln("Agent not available: %s".formatted(e.getMessage()));
                    } finally {
                        pw.flush();
                    }
                });
    }
}
