package io.battery.scenario.step;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import io.battery.AbstractIntegrationTest;
import io.battery.ProfileNames;
import io.battery.model.Step;
import io.battery.scenario.CancellationMarker;

/**
 * Verifies that steps run by the configured step runner inside a transaction template
 * participate in that transaction, as transactional scenarios assume. Spring transactions
 * are bound to the calling thread, so steps must run on the calling thread.
 */
@ActiveProfiles({
        ProfileNames.OFFLINE,
        ProfileNames.NOSHELL,
        "demo-crud",
        "dev"})
public class StepRunnerTransactionTest extends AbstractIntegrationTest {
    @Autowired
    private StepRunner stepRunner;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Step step(String name, String sql) {
        Step step = new Step();
        step.setName(name);
        step.setSql(sql);
        step.setCapture(true);
        return step;
    }

    @Test
    public void givenTransactionalSteps_thenStepsParticipateInCallerTransaction() {
        List<Step> steps = List.of(
                step("tx probe one", "select txid_current() as tx1, pg_backend_pid() as pid1"),
                step("tx probe two", "select txid_current() as tx2, pg_backend_pid() as pid2"));

        // Same calls as the work simulator for transactional scenarios
        transactionTemplate.executeWithoutResult(status -> {
            long callerTxId = jdbcTemplate.queryForObject("select txid_current()", Long.class);
            int callerPid = jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class);

            Map<String, Object> stepState = stepRunner.runSteps(steps, Map.of(), new CancellationMarker());

            Assertions.assertThat(stepState)
                    .containsEntry("tx1", callerTxId)
                    .containsEntry("tx2", callerTxId)
                    .containsEntry("pid1", callerPid)
                    .containsEntry("pid2", callerPid);
        });
    }
}
