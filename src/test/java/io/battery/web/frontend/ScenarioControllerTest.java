package io.battery.web.frontend;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import io.battery.metrics.Problem;
import io.battery.model.BatteryModel;
import io.battery.model.Scenario;
import io.battery.scenario.ScenarioLauncher;
import io.battery.scenario.ScenarioListener;
import io.battery.scenario.ScenarioStatus;
import io.battery.scenario.run.RunRecorder;
import io.battery.scenario.run.RunSummary;
import io.battery.web.frontend.model.ScenarioForm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit-test")
public class ScenarioControllerTest {
    private ScenarioListener scenarioListener;

    private RunRecorder runRecorder;

    private ScenarioController scenarioController;

    @BeforeEach
    public void setUp() {
        BatteryModel batteryModel = mock(BatteryModel.class);
        SimpMessagePublisher messagePublisher = mock(SimpMessagePublisher.class);
        scenarioListener = mock(ScenarioListener.class);
        ScenarioLauncher scenarioLauncher = mock(ScenarioLauncher.class);
        runRecorder = mock(RunRecorder.class);

        scenarioController = new ScenarioController();
        ReflectionTestUtils.setField(scenarioController, "batteryModel", batteryModel);
        ReflectionTestUtils.setField(scenarioController, "messagePublisher", messagePublisher);
        ReflectionTestUtils.setField(scenarioController, "scenarioListener", scenarioListener);
        ReflectionTestUtils.setField(scenarioController, "scenarioLauncher", scenarioLauncher);
        ReflectionTestUtils.setField(scenarioController, "runRecorder", runRecorder);
    }

    @Test
    public void testIndexPage() throws Exception {
        when(scenarioListener.getActiveStatus()).thenReturn(ScenarioStatus.RUNNING);
        Scenario scenario = mock(Scenario.class);
        when(scenario.describe()).thenReturn("Test scenario description");
        when(scenarioListener.getActiveScenario()).thenReturn(Optional.of(scenario));
        when(scenarioListener.getLastError()).thenReturn(Optional.empty());

        Model model = new ConcurrentModel();
        ScenarioForm form = new ScenarioForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");

        String viewName = scenarioController.indexPage(form, bindingResult, model).call();

        assertThat(viewName).isEqualTo("scenario");
        assertThat(model.getAttribute("activeStatus")).isEqualTo(ScenarioStatus.RUNNING);
        assertThat(model.getAttribute("activeTitle")).isEqualTo("Test scenario description");
        assertThat(model.containsAttribute("lastError")).isFalse();
    }

    @Test
    public void testStatusPanelFragment() throws Exception {
        when(scenarioListener.getActiveStatus()).thenReturn(ScenarioStatus.FAILED);
        when(scenarioListener.getActiveScenario()).thenReturn(Optional.empty());
        Problem error = mock(Problem.class);
        when(scenarioListener.getLastError()).thenReturn(Optional.of(error));

        Model model = new ConcurrentModel();
        String viewName = scenarioController.statusPanel(model).call();

        assertThat(viewName).isEqualTo("scenario :: statusPanel");
        assertThat(model.getAttribute("activeStatus")).isEqualTo(ScenarioStatus.FAILED);
        assertThat(model.getAttribute("activeTitle")).isEqualTo("No active scenario");
        assertThat(model.getAttribute("lastError")).isEqualTo(error);
    }

    @Test
    public void testStatusPanelWithLastRunWhenIdle() throws Exception {
        RunSummary lastRun = mock(RunSummary.class);
        when(runRecorder.getLastRun()).thenReturn(Optional.of(lastRun));
        when(scenarioListener.getActiveStatus()).thenReturn(ScenarioStatus.COMPLETED);
        when(scenarioListener.getActiveScenario()).thenReturn(Optional.empty());
        when(scenarioListener.getLastError()).thenReturn(Optional.empty());

        Model model = new ConcurrentModel();
        scenarioController.statusPanel(model).call();

        assertThat(model.getAttribute("lastRun")).isEqualTo(lastRun);
    }

    @Test
    public void testStatusPanelWithoutLastRunWhileRunning() throws Exception {
        when(runRecorder.getLastRun()).thenReturn(Optional.of(mock(RunSummary.class)));
        when(scenarioListener.getActiveStatus()).thenReturn(ScenarioStatus.RUNNING);
        when(scenarioListener.getActiveScenario()).thenReturn(Optional.of(mock(Scenario.class)));
        when(scenarioListener.getLastError()).thenReturn(Optional.empty());

        Model model = new ConcurrentModel();
        scenarioController.statusPanel(model).call();

        // The previous run's summary would be mistaken for the active run's
        assertThat(model.containsAttribute("lastRun")).isFalse();
    }
}
