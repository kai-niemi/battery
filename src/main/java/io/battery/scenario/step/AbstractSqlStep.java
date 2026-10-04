package io.battery.scenario.step;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import io.battery.model.Step;
import io.battery.script.BatteryScript;
import io.battery.script.Constants;
import io.battery.util.PlaceholderUtils;

/**
 * Base class for SQL step actions, executing statements with named parameters resolved by
 * evaluating battery script expressions. A single-row result is merged column by column into
 * the state, while a multi-row result is stored as a list under the step result name.
 * The resulting state is returned only for capturing steps, otherwise the input state passes through.
 */
public abstract class AbstractSqlStep implements StepAction<Map<String, Object>> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BatteryScript batteryScript;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public AbstractSqlStep(BatteryScript batteryScript, DataSource dataSource) {
        this.batteryScript = batteryScript;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    protected final Map<String, Object> doPerform(Step step,
                                                  Map<String, Object> initialState,
                                                  List<String> sqlStatements) {
        Map<String, Object> compositeResult = new LinkedHashMap<>(initialState);

        for (String sql : sqlStatements) {
            executeStatement(sql, compositeResult, step);
        }

        if (step.isCapture()) {
            return compositeResult;
        }

        // Not capturing means this step contributes nothing, not that it wipes out
        // the state captured by preceding steps.
        return initialState;
    }

    private void executeStatement(String sql,
                                  Map<String, Object> compositeResult,
                                  Step step) {
        String statement = expandPlaceholders(sql, compositeResult);

        if (logger.isDebugEnabled()) {
            logger.debug("Executing SQL statement [%s] with params %s"
                    .formatted(statement, step.getParams()));
        }

        namedParameterJdbcTemplate.execute(statement,
                new SqlParameterSource() {
                    @Override
                    public boolean hasValue(String paramName) {
                        return step.getParams().containsKey(paramName);
                    }

                    @Override
                    public @Nullable Object getValue(String paramName) throws IllegalArgumentException {
                        String expression = step.getParams().get(paramName);

                        Object paramValue = executeScript(expression, compositeResult);

                        if (logger.isDebugEnabled()) {
                            logger.debug("Resolving parameter [%s] value using expression [%s]: [%s]"
                                    .formatted(paramName, expression, paramValue));
                        }

                        return paramValue;
                    }
                },
                stmt -> {
                    stmt.setFetchSize(1);

                    boolean hasResultSet = stmt.execute();
                    int updateCount = -1;

                    if (logger.isDebugEnabled()) {
                        logSqlWarnings(stmt);
                    }

                    do {
                        if (hasResultSet) {
                            try (ResultSet rs = stmt.getResultSet()) {
                                List<Map<String, Object>> rv = extractResult(rs);
                                if (rv.size() == 1) {
                                    compositeResult.putAll(rv.getFirst());
                                } else {
                                    compositeResult.put(step.getResultName(), rv);
                                }
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug("ResultSet returned for SQL:%n%s"
                                        .formatted(statement));
                            }
                        } else {
                            updateCount = stmt.getUpdateCount();
                            if (updateCount >= 0 && logger.isDebugEnabled()) {
                                logger.debug("Returned update count %d for SQL:%n%s"
                                        .formatted(updateCount, statement));
                            }
                        }
                        hasResultSet = stmt.getMoreResults();
                    } while (hasResultSet || updateCount != -1);

                    return null;
                });
    }

    private String expandPlaceholders(String statement,
                                      Map<String, Object> compositeResult) {
        if (logger.isDebugEnabled()) {
            logger.debug("Expanding placeholders [" + statement + "]");
        }

        // First expand any placeholders in SQL statement
        String expanded = PlaceholderUtils.expandPlaceholders(statement, compositeResult);

        expanded = PlaceholderUtils.expandEmbeddings(expanded,
                expression -> "" + executeScript(expression, compositeResult));

        if (logger.isDebugEnabled()) {
            logger.debug("Expanded placeholders [" + expanded + "]");
        }

        return expanded;
    }

    private Object executeScript(String script,
                                 Map<String, Object> compositeResult) {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing script [" + script + "]");
        }

        Map<String, Object> map = batteryScript.execute(script, compositeResult);

        Object result = map.get(Constants.LAST_RESULT_VAR);

        if (logger.isDebugEnabled()) {
            logger.debug("Executed script result [" + result + "]");
        }

        return result;
    }

    private void logSqlWarnings(Statement stmt) throws SQLException {
        SQLWarning warningToLog = stmt.getWarnings();
        while (warningToLog != null) {
            logger.debug("SQLWarning detected: SQL state '%s', error code '%d', message [%s]"
                    .formatted(warningToLog.getSQLState(), warningToLog.getErrorCode(),
                            warningToLog.getMessage()));
            warningToLog = warningToLog.getNextWarning();
        }
    }

    private List<Map<String, Object>> extractResult(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<Map<String, Object>> rows = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i); // Use label to support aliases
                Object columnValue = rs.getObject(i);
                row.put(columnName, columnValue);
            }
            rows.add(row);
        }

        return rows;
    }
}

