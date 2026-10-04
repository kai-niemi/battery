package io.battery.script.function.support;

import java.util.Map;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import io.battery.AbstractIntegrationTest;
import io.battery.ProfileNames;
import io.battery.script.function.DefaultJdbcFunctions;

@ActiveProfiles({
        ProfileNames.OFFLINE,
        ProfileNames.NOSHELL,
        "demo-crud",
        "dev"})
public class DefaultJdbcFunctionsTest extends AbstractIntegrationTest {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    public void setup() {
        jdbcTemplate.execute("create schema if not exists demo");
        jdbcTemplate.execute("drop table if exists demo.battery_test_keys");
        jdbcTemplate.execute("create table demo.battery_test_keys (id serial primary key, name text default 'x')");
    }

    @AfterAll
    public void teardown() {
        jdbcTemplate.execute("drop table if exists demo.battery_test_keys");
    }

    @Test
    public void givenInsert_whenUpdateForKeys_expectGeneratedKeys() {
        DefaultJdbcFunctions functions = new DefaultJdbcFunctions(dataSource);

        Map<String, Object> first = functions.updateForKeys("insert into demo.battery_test_keys default values");
        Map<String, Object> second = functions.updateForKeys("insert into demo.battery_test_keys default values");

        Assertions.assertThat(first).containsKey("id");
        Assertions.assertThat(((Number) second.get("id")).longValue())
                .isGreaterThan(((Number) first.get("id")).longValue());
    }

    @Test
    public void givenTypeNamesInLowerCase_expectResolved() {
        DefaultJdbcFunctions functions = new DefaultJdbcFunctions(dataSource);

        Map<String, Object> keys = functions.updateForKeys(
                "insert into demo.battery_test_keys (name) values (?)",
                new Object[] {"lower"}, new String[] {"varchar"});

        Assertions.assertThat(keys).containsKey("id");
    }

    @Test
    public void givenEmptyArrayArgument_expectClearError() {
        DefaultJdbcFunctions functions = new DefaultJdbcFunctions(dataSource);

        Assertions.assertThatThrownBy(() -> functions.updateForKeys(
                        "insert into demo.battery_test_keys (name) select unnest(?::text[])",
                        new Object[] {new Object[0]}, new String[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Cannot infer the SQL array type of argument 1");
    }
}
