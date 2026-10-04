package io.battery.scenario.step;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.battery.model.ScriptFormat;
import io.battery.model.Step;
import io.battery.script.BatteryScript;
import io.battery.util.SqlStatements;

/**
 * Step action executing the SQL statements of a SQL file given by the step path.
 */
@Component
public class SqlFileStep extends AbstractSqlStep {
    public SqlFileStep(BatteryScript batteryScript, DataSource dataSource) {
        super(batteryScript, dataSource);
    }

    @Override
    public boolean accepts(Step step) {
        return !StringUtils.hasLength(step.getSql())
               && Objects.nonNull(step.getPath())
               && ScriptFormat.SQL.accepts(step.getPath());
    }

    @Override
    public final Map<String, Object> perform(Step step, Map<String, Object> initialState) {
        try {
            List<String> statements = SqlStatements.parse(Files.readString(step.getPath()));
            return doPerform(step, initialState, statements);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
