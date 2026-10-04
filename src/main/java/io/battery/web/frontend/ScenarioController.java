package io.battery.web.frontend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

import io.battery.ProfileNames;
import io.battery.event.CancelledEvent;
import io.battery.event.CompletedEvent;
import io.battery.event.PhaseProgressEvent;
import io.battery.event.RunFinishedEvent;
import io.battery.event.StartedEvent;
import io.battery.model.BatteryModel;
import io.battery.model.ScenarioExecutionException;
import io.battery.scenario.CancellationMarker;
import io.battery.scenario.ScenarioLauncher;
import io.battery.scenario.ScenarioListener;
import io.battery.scenario.ScenarioRequest;
import io.battery.scenario.run.RunRecorder;
import io.battery.util.PathUtils;
import io.battery.web.frontend.model.ProgressModel;
import io.battery.web.frontend.model.ProgressUpdateEvent;
import io.battery.web.frontend.model.ScenarioForm;
import io.battery.web.frontend.model.TopicName;

@WebController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping("/scenario")
public class ScenarioController {
    @Autowired
    private BatteryModel batteryModel;

    @Autowired
    private SimpMessagePublisher messagePublisher;

    @Autowired
    private ScenarioListener scenarioListener;

    @Autowired
    private ScenarioLauncher scenarioLauncher;

    @Autowired
    private RunRecorder runRecorder;

    private final ProgressModel progressModel = new ProgressModel();

    @EventListener
    public void handle(PhaseProgressEvent event) {
        progressModel.setPhaseProgress(event.getElement(), event.getProgress());

        messagePublisher.convertAndSend(TopicName.SCENARIO_PHASE_PROGRESS,
                new ProgressUpdateEvent(
                        event.getElement().getName().replace(" ", "_"),
                        event.getProgress()));
    }

    @EventListener
    public void handle(StartedEvent event) {
        messagePublisher.convertAndSend(TopicName.SCENARIO_PAGE_REFRESH, "");
    }

    @EventListener
    public void handle(CompletedEvent event) {
        progressModel.reset();
        messagePublisher.convertAndSend(TopicName.SCENARIO_PAGE_REFRESH, "");
    }

    @EventListener
    public void handle(CancelledEvent event) {
        progressModel.reset();
        messagePublisher.convertAndSend(TopicName.SCENARIO_PAGE_REFRESH, "");
    }

    // A run finishes once all its users complete, which may be after the completed event
    @EventListener
    public void handle(RunFinishedEvent event) {
        messagePublisher.convertAndSend(TopicName.SCENARIO_PAGE_REFRESH, "");
    }

    @ModelAttribute("progressModel")
    public ProgressModel progressModel() {
        return progressModel;
    }

    @ModelAttribute("batteryModel")
    public BatteryModel batteryModel() {
        return batteryModel;
    }

    @ModelAttribute("form")
    public ScenarioForm form() {
        try {
            ScenarioForm form = new ScenarioForm();
            form.setApplicationModelYaml(Files.readString(
                    PathUtils.listApplicationYamlFiles(batteryModel.getBaseDir())
                            .stream()
                            .findFirst()
                            .orElseThrow()));
            form.setName(batteryModel.getPrimaryScenario()
                    .orElse(batteryModel.getScenarios().getFirst()).getName());
            return form;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void populateStatusModel(Model model) {
        model.addAttribute("activeStatus", scenarioListener.getActiveStatus());

        scenarioListener.getLastError().ifPresent(scenarioError ->
                model.addAttribute("lastError", scenarioError));

        scenarioListener.getActiveRunId().ifPresent(runId ->
                model.addAttribute("activeRunId", runId));

        scenarioListener.getActiveScenario().ifPresentOrElse(scenario -> {
                    model.addAttribute("activeScenario", scenario);
                    model.addAttribute("activeTitle", scenario.describe());
                },
                () -> {
                    model.addAttribute("activeTitle", "No active scenario");
                    // While idle, show the summary of the most recent run
                    runRecorder.getLastRun().ifPresent(run -> model.addAttribute("lastRun", run));
                });
    }

    @GetMapping
    public Callable<String> indexPage(@ModelAttribute("form") ScenarioForm form,
                                      BindingResult bindingResult,
                                      Model model) {
        return () -> {
            try {
                populateStatusModel(model);
            } catch (ScenarioExecutionException e) {
                bindingResult.addError(new ObjectError("globalError", e.getMessage()));
            }

            return "scenario";
        };
    }

    @GetMapping("/status-panel")
    public Callable<String> statusPanel(Model model) {
        return () -> {
            populateStatusModel(model);
            return "scenario :: statusPanel";
        };
    }

    @PostMapping(params = "action=begin")
    public Callable<String> beginScenario(@ModelAttribute("form") @Valid ScenarioForm form,
                                          BindingResult bindingResult) {
        return () -> {
            if (scenarioListener.getActiveScenario().isPresent()) {
                bindingResult.addError(new ObjectError(
                        "globalError", "A scenario is already running!"));
            }

            if (bindingResult.hasErrors()) {
                return "scenario";
            }

            scenarioLauncher.launchNow(ScenarioRequest.newInstance()
                            .setName(form.getName())
                            .setSkipBeforeSteps(form.getSkipBeforeSteps())
                            .setSkipAfterSteps(form.getSkipAfterSteps())
                            .setSkipAllPhases(form.getSkipAllPhases())
                            .setSkipPhases(Arrays.stream(form.getSkipPhases()).collect(Collectors.toSet())),
                    new CancellationMarker());

            return "redirect:/scenario";
        };
    }

    @PostMapping(params = "action=abort")
    public Callable<String> abortScenario(@ModelAttribute("form") ScenarioForm form,
                                          BindingResult bindingResult) {
        return () -> {
            if (scenarioListener.getActiveScenario().isEmpty()) {
                bindingResult.addError(new ObjectError(
                        "globalError", "No scenario is running!"));
            } else {
                scenarioLauncher.cancelActiveScenario("by operator");
            }
            if (bindingResult.hasErrors()) {
                return "scenario";
            }
            return "redirect:/scenario";
        };
    }

    @PostMapping(params = "action=reset")
    public Callable<String> resetScenario(@ModelAttribute("form") ScenarioForm form,
                                          BindingResult bindingResult) {
        return () -> {
            if (scenarioListener.getActiveScenario().isPresent()) {
                bindingResult.addError(new ObjectError(
                        "globalError", "A scenario is still running!"));
            } else {
                progressModel.reset();
                scenarioListener.resetToIdle();
            }
            if (bindingResult.hasErrors()) {
                return "scenario";
            }
            return "redirect:/scenario";
        };
    }
}
