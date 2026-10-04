package io.battery.scenario;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import io.battery.AbstractIntegrationTest;
import io.battery.ProfileNames;
import io.battery.model.BatteryModel;
import io.battery.script.BatteryScript;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({
        ProfileNames.OFFLINE,
        ProfileNames.NOSHELL,
        "demo-crud",
        "dev"})
public class CrudScenarioTest extends AbstractIntegrationTest {
    @Autowired
    private BatteryScript batteryScript;

    @Autowired
    private BatteryModel batteryModel;

    @Test
    public void givenModel_whenInspectingBasics_thenDataMatch() {
        assertThat(batteryModel.getBefore().getSteps()).hasSize(1);
        assertThat(batteryModel.getAfter().getSteps()).hasSize(0);
        assertThat(batteryModel.getScenarios()).hasSize(2);
        assertThat(batteryModel.getPhases()).hasSize(1);
    }

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
    public void givenPaginationQuery_thenReturnEachPage() {
        String statement = """
                rows = 0;
                pages = 0;
                _list = jdbc.queryForList("select * from demo.customer order by id limit 10");
                while (!_list.isEmpty()) {
                    pages = pages + 1;
                    _lastId = null;
                    for _idx from 0 to _list.size()-1 {
                        rows = rows + 1;
                        _map = _list.get(_idx);
                        _lastId = _map.get("id");
                    }
                    _list = jdbc.queryForList("select * from demo.customer where id > ? order by id limit 10", [_lastId]);
                }
                """;
        Map<String, Object> rv = batteryScript.execute(statement);
//        assertThat(rv).containsEntry("rows", 30);
//        assertThat(rv).containsEntry("pages", 3);
    }

    @Test
    public void givenPaginationQueryUsingForeach_thenReturnEachPage() {
        String statement = """
                rows = 0;
                pages = 0;
                _list = jdbc.queryForList("select * from demo.customer order by id limit ?", [10]);
                while (!_list.isEmpty()) {
                    pages = pages + 1;
                    _lastId = null;
                    foreach (_list) {
                        rows = rows + 1;
                        _lastId = _x.get("id");
                    }
                    _list = jdbc.queryForList("select * from demo.customer where id > ? order by id limit 10", [_lastId]);
                }
                """;
        Map<String, Object> rv = batteryScript.execute(statement);
//        assertThat(rv).containsEntry("rows", 30);
//        assertThat(rv).containsEntry("pages", 3);
    }
}
