package io.battery.scenario;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import io.battery.AbstractIntegrationTest;
import io.battery.ProfileNames;
import io.battery.model.After;
import io.battery.model.BatteryModel;
import io.battery.model.Before;
import io.battery.model.Phase;
import io.battery.model.Scenario;
import io.battery.model.Step;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({
        ProfileNames.OFFLINE,
        ProfileNames.NOSHELL,
        "basic",
        "dev"})
public class BasicScenarioTest extends AbstractIntegrationTest {
    @Autowired
    private BatteryModel batteryModel;

    @Autowired
    private ScenarioLauncher scenarioLauncher;

    @Test
    public void givenModel_whenLaunchingScenarioLater_thenWaitUntilCompletion() {
        scenarioLauncher.launchNowAndWait(
                ScenarioRequest.newInstance().setName("Inserts using Explicit Transactions"),
                new CancellationMarker());
    }

    @Test
    public void givenModel_whenLaunchingScenarioNow_thenWaitUntilCompletion() {
        scenarioLauncher.launchNow(
                        ScenarioRequest.newInstance().setName("Inserts using Explicit Transactions"),
                        new CancellationMarker())
                .join();
    }

    @Test
    public void givenModel_whenInspectingBasics_thenDataMatch() {
        assertThat(batteryModel.getBefore().getSteps()).hasSize(1);
        assertThat(batteryModel.getAfter().getSteps()).hasSize(0);
        assertThat(batteryModel.getScenarios()).hasSize(2);
        assertThat(batteryModel.getPhases()).hasSize(3);
    }

    @Test
    public void givenModel_whenInspectingBefore_thenDataMatch() {
        Before before = batteryModel.getBefore();
        assertThat(before.getSteps()).hasSize(1);
        assertThat(before.getSteps().get(0).getName()).isEqualTo("Prepare Schema");
        assertThat(before.getSteps().get(0).getSql()).isNotBlank();
    }

    @Test
    public void givenModel_whenInspectingAfter_thenDataMatch() {
        After after = batteryModel.getAfter();
        assertThat(after.getSteps()).hasSize(0);
    }

    @Test
    public void givenModel_whenInspectingPhases_thenDataMatch() {
        List<Phase> phases = batteryModel.getPhases();
        assertThat(phases).hasSize(3);

        assertThat(phases)
                .filteredOn(phase -> phase.getName().equals("Warm up phase"))
                .hasSize(1)
                .first()
                .extracting(Phase::getDuration, Phase::getStartRate, Phase::getMaxRate)
                .containsExactly(Duration.ofSeconds(30), 1, 15);

        assertThat(phases)
                .filteredOn(phase -> phase.getName().equals("Steady phase"))
                .hasSize(1)
                .first()
                .extracting(Phase::getDuration, Phase::getStartRate, Phase::getMaxRate)
                .containsExactly(Duration.ofMinutes(2), 0, 0);

        assertThat(phases)
                .filteredOn(phase -> phase.getName().equals("Peak phase"))
                .hasSize(1)
                .first()
                .extracting(Phase::getDuration, Phase::getStartRate, Phase::getMaxRate)
                .containsExactly(Duration.ofSeconds(30), 4, 30);
    }

    @Test
    public void givenModel_whenInspectingScenarioOne_thenDataMatch() {
        List<Scenario> scenarios = batteryModel.getScenarios();
        assertThat(scenarios).hasSize(2);

        assertThat(scenarios)
                .filteredOn(scenario -> scenario.getName().equals("Inserts using Explicit Transactions"))
                .hasSize(1)
                .first()
                .extracting(Scenario::getDuration, Scenario::getWeight, Scenario::isTransactional)
                .containsExactly(Duration.ofMinutes(2), 1.0, true);

        List<Step> steps =
                assertThat(scenarios)
                        .filteredOn(scenario -> scenario.getName().equals("Inserts using Explicit Transactions"))
                        .hasSize(1)
                        .first()
                        .extracting(Scenario::getSteps)
                        .actual();
        assertThat(steps)
                .filteredOn(step -> step.getName().equals("Insert tokens"))
                .hasSize(1)
                .first()
                .extracting(Step::getSql)
                .matches(s -> {
                    return s.startsWith("insert");
                });
    }

    @Test
    public void givenModel_whenInspectingScenarioTwo_thenDataMatch() {
        List<Scenario> scenarios = batteryModel.getScenarios();
        assertThat(scenarios).hasSize(2);

        assertThat(scenarios)
                .filteredOn(scenario -> scenario.getName().equals("Inserts using Implicit Transactions"))
                .hasSize(1)
                .first()
                .extracting(Scenario::getDuration, Scenario::getWeight, Scenario::isTransactional)
                .containsExactly(Duration.ofMinutes(2), 1.0, false);

        List<Step> steps =
                assertThat(scenarios)
                        .filteredOn(scenario -> scenario.getName().equals("Inserts using Implicit Transactions"))
                        .hasSize(1)
                        .first()
                        .extracting(Scenario::getSteps)
                        .actual();
        assertThat(steps)
                .filteredOn(step -> step.getName().equals("Insert tokens"))
                .hasSize(1)
                .first()
                .extracting(Step::getSql)
                .matches(s -> {
                    return s.startsWith("insert");
                });
    }
}
