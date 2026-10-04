package io.battery.repository;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
public class PostgresRepository implements MetadataRepository {
    protected final JdbcTemplate jdbcTemplate;

    public PostgresRepository(@Autowired DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public String databaseName() {
        return jdbcTemplate.queryForObject("SELECT current_database()", String.class);
    }

    @Override
    public String databaseVersion() {
        return jdbcTemplate.queryForObject("select version()", String.class);
    }

    @Override
    public String databaseIsolation() {
        return jdbcTemplate
                .queryForObject("SHOW transaction_isolation", String.class);
    }
}
