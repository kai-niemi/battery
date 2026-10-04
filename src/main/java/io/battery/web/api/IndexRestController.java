package io.battery.web.api;

import java.io.IOException;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.battery.ProfileNames;
import io.battery.web.api.model.LinkRelations;
import io.battery.web.api.model.MessageModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping(value = "/api")
public class IndexRestController {
    @GetMapping
    public ResponseEntity<MessageModel> index() {
        MessageModel index = new MessageModel();
        index.setMessage("Battery API index resource");
        index.add(linkTo(methodOn(IndexRestController.class).index())
                        .withSelfRel())
                .add(Link.of(ServletUriComponentsBuilder.fromCurrentContextPath()
                                .pathSegment("api", "actuator")
                                .buildAndExpand()
                                .toUriString())
                        .withRel(LinkRelations.ACTUATORS_REL)
                        .withTitle("Spring boot actuators"))
                .add(linkTo(methodOn(ScenarioRestController.class)
                        .findScenarios())
                        .withRel(LinkRelations.SCENARIOS_REL)
                        .withTitle("Scenario control resource"))
                .add(linkTo(methodOn(WorkerRestController.class)
                        .findWorkers(Optional.empty(), Optional.empty()))
                        .withRel(LinkRelations.WORKERS_REL)
                        .withTitle("Virtual user workers resource")
                )
                .add(linkTo(methodOn(RunRestController.class)
                        .findRuns())
                        .withRel(LinkRelations.RUNS_REL)
                        .withTitle("Scenario run summaries resource")
                );

        return ResponseEntity.ok(index);
    }

    @GetMapping("/fake")
    public @ResponseBody ResponseEntity<MessageModel> errorOnGet() {
        throw new FakeException("Fake exception!", new IOException("I/O disturbance!"));
    }

    @PutMapping("/fake")
    public ResponseEntity<MessageModel> errorOnPut() {
        throw new FakeException("Fake exception!", new IOException("I/O disturbance!"));
    }

    @PostMapping("/fake")
    public ResponseEntity<MessageModel> errorOnPost() {
        throw new FakeException("Fake exception!", new IOException("I/O disturbance!"));
    }

    @DeleteMapping("/fake")
    public ResponseEntity<MessageModel> errorOnDelete() {
        throw new FakeException("Fake exception!", new IOException("I/O disturbance!"));
    }
}
