package io.battery.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.dialect.DialectResolver;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.dialect.PostgresDialect;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import io.battery.Application;
import io.battery.repository.CockroachRepository;
import io.battery.repository.MetadataRepository;
import io.battery.repository.PostgresRepository;

@Configuration
@EnableJdbcRepositories(basePackageClasses = {Application.class})
public class RepositoryConfig {
    @Bean
    public MetadataRepository metadataRepository(@Autowired DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        JdbcDialect dialect = DialectResolver.getDialect(jdbcTemplate);
        if (dialect instanceof PostgresDialect) {
            return createPostgresRepository(jdbcTemplate);
        }

        return jdbcTemplate.execute((ConnectionCallback<MetadataRepository>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase(Locale.ENGLISH);
            String version = metaData.getDatabaseProductVersion().toLowerCase(Locale.ENGLISH);
            int isolationLevel = metaData.getDefaultTransactionIsolation();

            return new MetadataRepository() {
                @Override
                public String databaseName() {
                    return productName;
                }

                @Override
                public String databaseVersion() {
                    return version;
                }

                @Override
                public String databaseIsolation() {
                    return switch (isolationLevel) {
                        case Connection.TRANSACTION_NONE -> "TRANSACTION_NONE";
                        case Connection.TRANSACTION_READ_UNCOMMITTED -> "TRANSACTION_READ_UNCOMMITTED";
                        case Connection.TRANSACTION_READ_COMMITTED -> "TRANSACTION_READ_COMMITTED";
                        case Connection.TRANSACTION_REPEATABLE_READ -> "TRANSACTION_REPEATABLE_READ";
                        case Connection.TRANSACTION_SERIALIZABLE -> "TRANSACTION_SERIALIZABLE";
                        default -> "UNKNOWN_ (" + isolationLevel + ")";
                    };
                }
            };
        });
    }

    private MetadataRepository createPostgresRepository(JdbcTemplate jdbcTemplate) {
        // Works for both CRDB and PSQL
        String v = jdbcTemplate.queryForObject("select version()", String.class);
        if (v != null && v.contains("CockroachDB")) {
            return new CockroachRepository(jdbcTemplate.getDataSource());
        }
        return new PostgresRepository(jdbcTemplate.getDataSource());
    }
}
