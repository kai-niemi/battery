package io.battery.repository;

import javax.sql.DataSource;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.SUPPORTS) // to support both explicit and implicit
public class CockroachRepository extends PostgresRepository {
    public CockroachRepository(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String databaseName() {
        return jdbcTemplate.queryForObject("SHOW database", String.class);
    }
}

