package io.battery.web.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.SimpleRepresentationModelAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import io.battery.ProfileNames;
import io.battery.model.BatteryModel;
import io.battery.model.Scenario;
import io.battery.scenario.CancellationMarker;
import io.battery.scenario.ScenarioLauncher;
import io.battery.scenario.ScenarioListener;
import io.battery.scenario.ScenarioRequest;
import io.battery.util.DigestUtils;
import io.battery.web.api.model.LinkRelations;
import io.battery.web.api.model.MessageModel;
import io.battery.web.api.model.MessageType;
import io.battery.web.api.model.StartScenarioForm;
import io.battery.web.api.model.StatusModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping(value = "/api/scenario")
public class ScenarioRestController {
    private final SimpleRepresentationModelAssembler<Scenario> SCENARIO_MODEL_ASSEMBLER
            = new SimpleRepresentationModelAssembler<>() {
        @Override
        public void addLinks(EntityModel<Scenario> resource) {
            resource.add(linkTo(methodOn(ScenarioRestController.class)
                    .findScenarioByName(resource.getContent().getName()))
                    .withSelfRel());
        }

        @Override
        public void addLinks(CollectionModel<EntityModel<Scenario>> resources) {
            resources.add(linkTo(methodOn(ScenarioRestController.class)
                            .findScenarios())
                            .withSelfRel())
                    .add(linkTo(methodOn(ScenarioRestController.class)
                            .getStatus())
                            .withRel(LinkRelations.STATUS_REL));

            scenarioListener.getActiveScenario().ifPresentOrElse(scenario -> {
                resources.add(linkTo(methodOn(ScenarioRestController.class)
                        .findScenarioByName(scenario.getName()))
                        .withRel(LinkRelations.CURRENT_REL));
                resources.add(linkTo(methodOn(ScenarioRestController.class)
                        .abortScenario())
                        .withRel(LinkRelations.ABORT_REL));
            }, () -> {
                resources.add(linkTo(methodOn(ScenarioRestController.class)
                        .startScenarioForm())
                        .withRel(LinkRelations.START_REL));
            });
        }
    };

    @Autowired
    private ScenarioLauncher scenarioLauncher;

    @Autowired
    private ScenarioListener scenarioListener;

    @Autowired
    private BatteryModel batteryModel;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.application.version}")
    private String appVersion;

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<Scenario>>> findScenarios() {
        return ResponseEntity.ok(SCENARIO_MODEL_ASSEMBLER
                .toCollectionModel(batteryModel.getScenarios()));
    }

    @GetMapping(value = "/{name}/detail")
    public ResponseEntity<EntityModel<Scenario>> findScenarioByName(@PathVariable String name) {
        Scenario scenario = batteryModel.findNamedScenario(name)
                .orElseThrow(() -> new NotFoundException("No such scenario: " + name));
        return ResponseEntity.ok(SCENARIO_MODEL_ASSEMBLER.toModel(scenario));
    }

    @GetMapping(value = "/status")
    public ResponseEntity<StatusModel> getStatus() {
        StatusModel statusModel = new StatusModel()
                .setScenarioStatus(scenarioListener.getActiveStatus())
                .setSecureHash(DigestUtils.toSecureHash(batteryModel))
                .setAppName(appName)
                .setAppVersion(appVersion)
                .add(linkTo(methodOn(ScenarioRestController.class)
                        .getStatus())
                        .withSelfRel());

        scenarioListener.getActiveScenario().ifPresent(scenario -> {
            statusModel.add(linkTo(methodOn(ScenarioRestController.class)
                    .findScenarioByName(scenario.getName()))
                    .withRel(LinkRelations.CURRENT_REL));
        });

        return ResponseEntity.ok(statusModel);
    }

    @GetMapping(value = "/start")
    public ResponseEntity<EntityModel<StartScenarioForm>> startScenarioForm() {
        StartScenarioForm requestForm = new StartScenarioForm();
        requestForm.setName(batteryModel.getPrimaryScenario()
                .orElse(batteryModel.getScenarios().getFirst())
                .getName());
        requestForm.setSecureHash(DigestUtils.toSecureHash(batteryModel));

        return ResponseEntity.ok(EntityModel.of(requestForm)
                .add(linkTo(methodOn(getClass())
                        .startScenarioForm())
                        .withSelfRel()
                        .andAffordance(afford(methodOn(getClass())
                                .startScenario(null))))
        );
    }

    @PostMapping("/start")
    public ResponseEntity<MessageModel> startScenario(@RequestBody @Valid StartScenarioForm request) {
        if (scenarioListener.getActiveScenario().isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.PRECONDITION_FAILED)
                    .body(MessageModel.from("A scenario is already running"));
        }

        if (StringUtils.hasLength(request.getSecureHash())) {
            if (!DigestUtils.toSecureHash(batteryModel).equals(request.getSecureHash())) {
                return ResponseEntity
                        .status(HttpStatus.PRECONDITION_FAILED)
                        .body(MessageModel.from("Client and host secure hash mismatch!")
                                .setMessageType(MessageType.error));
            }
        }

        scenarioLauncher.launchNow(ScenarioRequest.newInstance()
                        .setName(request.getName())
                        .setSkipAllPhases(request.isSkipPhases())
                        .setSkipAfterSteps(request.isSkipAfterSteps())
                        .setSkipBeforeSteps(request.isSkipBeforeSteps()),
                new CancellationMarker());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(MessageModel.from("Started scenario")
                        .add(linkTo(methodOn(getClass())
                                .findScenarioByName(request.getName()))
                                .withRel(LinkRelations.CURRENT_REL)));
    }

    @PostMapping("/abort")
    public ResponseEntity<MessageModel> abortScenario() {
        if (scenarioListener.getActiveScenario().isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.PRECONDITION_FAILED)
                    .body(MessageModel.from("No scenario is running"));
        }
        scenarioLauncher.cancelActiveScenario("by API endpoint");
        return ResponseEntity.ok()
                .body(new MessageModel("Aborted")
                        .setMessageType(MessageType.warning));
    }
}
