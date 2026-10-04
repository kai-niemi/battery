package io.battery.scenario.step;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.battery.model.Step;
import io.battery.script.BatteryScript;
import io.battery.util.SqlStatements;

/**
 * Step action executing the inline SQL statements of a step.
 */
@Component
public class SqlInlineStep extends AbstractSqlStep {
    public SqlInlineStep(BatteryScript batteryScript, DataSource dataSource) {
        super(batteryScript, dataSource);
    }

    @Override
    public boolean accepts(Step step) {
        return StringUtils.hasLength(step.getSql())
               && Objects.isNull(step.getPath());
    }

    @Override
    public final Map<String, Object> perform(Step step, Map<String, Object> initialState) {
        List<String> statements = SqlStatements.parse(step.getSql());
        return doPerform(step, initialState, statements);
    }
}
