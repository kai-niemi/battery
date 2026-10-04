package io.battery.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.shell.core.command.availability.Availability;
import org.springframework.shell.core.command.availability.AvailabilityProvider;
import org.springframework.web.client.ResourceAccessException;

import io.battery.model.BatteryModel;
import io.battery.model.Scenario;
import io.battery.shell.support.ListTableModel;
import io.battery.shell.support.ShellSupport;
import io.battery.util.AnsiPrintWriter;
import io.battery.util.DigestUtils;
import io.battery.web.HypermediaClient;
import io.battery.web.api.model.LinkRelations;
import io.battery.web.api.model.MessageModel;
import io.battery.web.api.model.StartScenarioForm;
import io.battery.web.api.model.StatusModel;
import static io.battery.util.AnsiPrintWriter.wrap;
import static io.battery.web.api.model.LinkRelations.ABORT_REL;
import static io.battery.web.api.model.LinkRelations.ACTUATORS_REL;
import static io.battery.web.api.model.LinkRelations.CURIE_NAMESPACE;
import static io.battery.web.api.model.LinkRelations.SCENARIOS_REL;
import static io.battery.web.api.model.LinkRelations.STATUS_REL;
import static org.springframework.hateoas.mediatype.hal.HalLinkRelation.curied;

@ShellComponent
public class AgentScenarioCommands extends AbstractShellCommand {
    @Autowired
    private HypermediaClient hypermediaClient;

    @Autowired
    private BatteryModel batteryModel;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.application.version}")
    private String appVersion;

    @Bean
    public AvailabilityProvider ifAgents() {
        return () -> batteryModel.getNetwork().getAgents().isEmpty()
                ? Availability.unavailable("no agents configured!")
                : Availability.available();
    }

    @Command(value = "Ping remote agent(s)",
            name = {"agent", "ping"},
            alias = {"ap"},
            group = CommandGroups.AGENT_COMMANDS,
            availabilityProvider = "ifAgents",
            exitStatusExceptionMapper = "commandExceptionMapper",
            completionProvider = "agentProvider")
    public void ping(@Option(description = "agent name or empty to include all") String name,
                     CommandContext commandContext) {
        final String secureHash = DigestUtils.toSecureHash(batteryModel);
        List<Object> titles = new ArrayList<>();
        List<List<?>> tuples = new ArrayList<>();

        AnsiPrintWriter pw = wrap(commandContext.outputWriter());

        batteryModel.getNetwork().findAgents(name)
                .forEach(agent -> {
                    try {
                        // Find agent build info via actuator
                        Map<String, Object> response = hypermediaClient.from(agent.indexLink())
                                .follow(curied(CURIE_NAMESPACE, ACTUATORS_REL).value())
                                .follow(HalLinkRelation.uncuried("info").value())
                                .toObject("$.build");
                        if (titles.isEmpty()) {
                            titles.addAll(response.keySet());
                        }
                        tuples.add(new ArrayList<>(response.values()));
                    } catch (ResourceAccessException e) {
                        pw.redln("Agent not available: %s".formatted(e.getMessage()));
                    }
                });

        pw.println("App name: #(by)%s#(d)".formatted(appName));
        pw.println("App version: #(by)%s#(d)".formatted(appVersion));
        pw.println("Secure hash: #(by)%s#(d)".formatted(secureHash));
        pw.println();
        pw.println(ShellSupport.prettyPrint(new ListTableModel<>(tuples, titles, List::get)));
    }

    @Command(value = "Run a named or random scenario on agent(s)",
            name = {"agent", "run"},
            alias = {"ar"},
            group = CommandGroups.AGENT_COMMANDS,
            availabilityProvider = "ifAgents",
            completionProvider = "agentAndScenarioProvider",
            exitStatusExceptionMapper = "commandExceptionMapper")
    public void runAgent(@Option(description = "agent name or empty to include all") String name,
                         @Option(description = "scenario name or alias") String scenario,
                         @Option(description = "skip all before steps", defaultValue = "false") boolean skipBefore,
                         @Option(description = "skip all after steps", defaultValue = "false") boolean skipAfter,
                         @Option(description = "skip all phases and just start a single virtual user (for testing)",
                                 defaultValue = "false") boolean skipPhases,
                         CommandContext commandContext) {
        AnsiPrintWriter pw = wrap(commandContext.outputWriter());

        StartScenarioForm requestForm = new StartScenarioForm();
        requestForm.setName(scenario);
        requestForm.setSkipBeforeSteps(skipBefore);
        requestForm.setSkipAfterSteps(skipAfter);
        requestForm.setSkipPhases(skipPhases);
        requestForm.setSecureHash(DigestUtils.toSecureHash(batteryModel));

        batteryModel.getNetwork().findAgents(name)
                .forEach(agent -> {
                    try {
                        pw.whiteln("Calling agent: %s".formatted(agent.getUrl()));

                        CollectionModel<?> form = hypermediaClient.from(agent.indexLink())
                                .follow(curied(CURIE_NAMESPACE, SCENARIOS_REL).value())
                                .toObject(CollectionModel.class);

                        ResponseEntity<MessageModel> response = hypermediaClient.post(
                                Objects.requireNonNull(form).getRequiredLink(LinkRelations.START_REL),
                                requestForm, MessageModel.class);

                        if (response.getStatusCode().is2xxSuccessful()) {
                            pw.greenln("HTTP status: %s".formatted(response.getStatusCode()));
                        } else {
                            pw.yellowln("Unexpected HTTP status: %s".formatted(response.getStatusCode()));
                        }
                    } catch (ResourceAccessException e) {
                        pw.redln("Agent not available: %s".formatted(e.getMessage()));
                    }
                });
    }

    @Command(value = "Show scenario status on agent(s)",
            name = {"agent", "status"},
            alias = {"as"},
            group = CommandGroups.AGENT_COMMANDS,
            availabilityProvider = "ifAgents",
            exitStatusExceptionMapper = "commandExceptionMapper",
            completionProvider = "agentProvider")
    public void statusAgent(@Option(description = "agent name or empty to include all") String name,
                            CommandContext commandContext) {
        final List<List<?>> tuples = new ArrayList<>();
        final String secureHash = DigestUtils.toSecureHash(batteryModel);

        AnsiPrintWriter pw = wrap(commandContext.outputWriter());

        batteryModel.getNetwork().findAgents(name)
                .forEach(agent -> {
                    try {
                        pw.whiteln("Calling agent: %s".formatted(agent.getUrl()));

                        ResponseEntity<StatusModel> entity = hypermediaClient.from(agent.indexLink())
                                .follow(curied(CURIE_NAMESPACE, SCENARIOS_REL).value())
                                .follow(curied(CURIE_NAMESPACE, STATUS_REL).value())
                                .toEntity(StatusModel.class);

                        StatusModel statusModel = entity.getBody();

                        tuples.add(List.of(
                                agent.getUrl(),
                                statusModel.getAppName(),
                                statusModel.getAppVersion(),
                                appVersion.equals(statusModel.getAppVersion()) ? "OK" : "Differs",
                                statusModel.getSecureHash(),
                                secureHash.equals(statusModel.getSecureHash()) ? "OK" : "Differs",
                                statusModel.getScenarioStatus()
                        ));
                    } catch (ResourceAccessException e) {
                        pw.redln("Agent not available: %s".formatted(e.getMessage()));
                    }
                });

        pw.println("App name: #(by)%s#(d)".formatted(appName));
        pw.println("App version: #(by)%s#(d)".formatted(appVersion));
        pw.println("Secure hash: #(by)%s#(d)".formatted(secureHash));
        pw.println();
        pw.println(ShellSupport.prettyPrint(new ListTableModel<>(tuples,
                List.of("URL", "Name", "Version", "Version Diff", "Hash", "Hash Diff", "Status"),
                List::get)));
    }

    @Command(value = "Cancel active scenario on agent(s)",
            name = {"agent", "cancel"},
            alias = {"ac"},
            availabilityProvider = "ifAgents",
            completionProvider = "agentProvider",
            group = CommandGroups.AGENT_COMMANDS,
            exitStatusExceptionMapper = "commandExceptionMapper")
    public void cancelScenario(@Option(description = "agent name or empty to include all") String name,
                               CommandContext commandContext) {
        final ParameterizedTypeReference<CollectionModel<EntityModel<Scenario>>>
                resourceParameterizedTypeReference = new ParameterizedTypeReference<>() {
        };

        AnsiPrintWriter pw = wrap(commandContext.outputWriter());

        batteryModel.getNetwork().findAgents(name)
                .forEach(agent -> {
                    try {
                        pw.whiteln("Cancel agent: %s".formatted(agent.getUrl()));

                        CollectionModel<?> model = hypermediaClient.from(agent.indexLink())
                                .follow(curied(CURIE_NAMESPACE, SCENARIOS_REL).value())
                                .toObject(resourceParameterizedTypeReference);

                        if (Objects.requireNonNull(model).hasLink(curied(CURIE_NAMESPACE, ABORT_REL))) {
                            ResponseEntity<MessageModel> response = hypermediaClient.post(
                                    model.getRequiredLink(curied(CURIE_NAMESPACE, ABORT_REL)),
                                    null, MessageModel.class);
                            if (response.getStatusCode().is2xxSuccessful()) {
                                pw.greenln("HTTP status: %s".formatted(response.getStatusCode()));
                            } else {
                                pw.yellowln("Unexpected HTTP status: %s".formatted(response.getStatusCode()));
                            }
                        } else {
                            pw.yellowln("No active scenario");
                        }
                    } catch (ResourceAccessException e) {
                        pw.redln("Agent not available: %s".formatted(e.getMessage()));
                    }
                });
    }
}
