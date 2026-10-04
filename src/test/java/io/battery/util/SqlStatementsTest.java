package io.battery.util;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit-test")
public class SqlStatementsTest {
    @Test
    public void testParseComplexSqlScript() {
        String script = """
                -- Initial comment
                CREATE TABLE "users""table" (
                    id INT PRIMARY KEY,
                    name VARCHAR(255) DEFAULT 'O''Connor; test'
                );
                
                /* Multi-line block comment
                   with ; semicolons */
                INSERT INTO "users""table" (id, name) VALUES (1, 'Alice');
                
                -- Trailing line comment at EOF
                SELECT * FROM "users""table" WHERE name = 'Alice'
                """;

        List<String> statements = SqlStatements.parse(script);
        assertThat(statements).hasSize(3);
        assertThat(statements.get(0)).startsWith("-- Initial comment\nCREATE TABLE");
        assertThat(statements.get(1)).startsWith("/* Multi-line block comment");
        assertThat(statements.get(2)).startsWith("-- Trailing line comment at EOF\nSELECT");
    }

    @Test
    public void testParseEmptyAndBlank() {
        List<String> empty = SqlStatements.parse("   ; ; \n\t ; ");
        assertThat(empty).isEmpty();
    }

    @Test
    public void testDollarQuotedStrings() {
        String script = """
                CREATE FUNCTION inc(i int) RETURNS int AS $$
                BEGIN
                    RETURN i + 1; -- semicolons in the body
                END;
                $$ LANGUAGE plpgsql;
                DO $body$ BEGIN PERFORM 'x;'; END $body$;
                SELECT $1, a$b$c FROM t WHERE x = $2;
                SELECT 1
                """;

        List<String> statements = SqlStatements.parse(script);

        assertThat(statements).hasSize(4);
        assertThat(statements.get(0)).startsWith("CREATE FUNCTION").endsWith("$$ LANGUAGE plpgsql");
        assertThat(statements.get(1)).isEqualTo("DO $body$ BEGIN PERFORM 'x;'; END $body$");
        // Positional parameters and $ in identifiers aren't dollar quotes
        assertThat(statements.get(2)).isEqualTo("SELECT $1, a$b$c FROM t WHERE x = $2");
        assertThat(statements.get(3)).isEqualTo("SELECT 1");
    }

    @Test
    public void testUnclosedDollarQuoteThrows() {
        assertThatThrownBy(() -> SqlStatements.parse("DO $$ BEGIN; END;"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SQL script ends inside a dollar-quoted string");
    }

    @Test
    public void testUnclosedTokensThrow() {
        assertThatThrownBy(() -> SqlStatements.parse("SELECT 'unclosed string"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SqlStatements.parse("SELECT \"unclosed identifier"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SqlStatements.parse("SELECT /* unclosed comment"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
