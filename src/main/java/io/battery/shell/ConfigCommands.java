package io.battery.shell;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

import io.battery.model.BatteryModel;
import io.battery.model.Root;
import io.battery.repository.MetadataRepository;
import io.battery.shell.support.ListTableModel;
import io.battery.shell.support.ShellSupport;
import io.battery.util.AsciiArt;
import static io.battery.util.AnsiPrintWriter.wrap;

@ShellComponent
public class ConfigCommands extends AbstractShellCommand {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConnectionPoolState {
        public int activeConnections;

        public int idleConnections;

        public int threadsAwaitingConnection;

        public int totalConnections;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConnectionPoolConfig {
        public long connectionTimeout;

        public String poolName;

        public long idleTimeout;

        public long leakDetectionThreshold;

        public int maximumPoolSize;

        public long maxLifetime;

        public int minimumIdle;

        public long validationTimeout;
    }

    private static ConnectionPoolState from(HikariPoolMXBean bean) {
        ConnectionPoolState instance = new ConnectionPoolState();
        instance.activeConnections = bean.getActiveConnections();
        instance.idleConnections = bean.getIdleConnections();
        instance.threadsAwaitingConnection = bean.getThreadsAwaitingConnection();
        instance.totalConnections = bean.getTotalConnections();
        return instance;
    }

    private static ConnectionPoolConfig from(HikariConfigMXBean bean) {
        ConnectionPoolConfig instance = new ConnectionPoolConfig();
        instance.connectionTimeout = bean.getConnectionTimeout();
        instance.poolName = bean.getPoolName();
        instance.idleTimeout = bean.getIdleTimeout();
        instance.leakDetectionThreshold = bean.getLeakDetectionThreshold();
        instance.maximumPoolSize = bean.getMaximumPoolSize();
        instance.maxLifetime = bean.getMaxLifetime();
        instance.minimumIdle = bean.getMinimumIdle();
        instance.validationTimeout = bean.getValidationTimeout();
        return instance;
    }

    @Autowired
    private HikariDataSource hikariDataSource;

    @Autowired
    private MetadataRepository metadataRepository;

    @Autowired
    private BatteryModel batteryModel;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private YAMLMapper yamlObjectMapper;

    private ConnectionPoolState getConnectionPoolSize() {
        return from(hikariDataSource.getHikariPoolMXBean());
    }

    private ConnectionPoolConfig getConnectionPoolConfig() {
        return from(hikariDataSource.getHikariConfigMXBean());
    }

    @Command(value = "Show database information",
            name = {"show", "db"},
            alias = {"sd"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.CONFIG_COMMANDS)
    public void databaseInfo(CommandContext ctx) {
        PrintWriter pw = wrap(ctx.outputWriter());

        List<List<Object>> data = new ArrayList<>();
        data.add(List.of("URL", hikariDataSource.getJdbcUrl()));
        data.add(List.of("Database", metadataRepository.databaseName()));
        data.add(List.of("Version", metadataRepository.databaseVersion()));
        data.add(List.of("Isolation", metadataRepository.databaseIsolation()));

        pw.println(ShellSupport.prettyPrint(
                new ListTableModel<>(data,
                        List.of("Name", "Value"),
                        List::get)));
    }

    @Command(value = "Show connection pool config",
            name = {"show", "pool", "config"},
            alias = {"spc"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.CONFIG_COMMANDS)
    public void showPoolConfig(CommandContext ctx) {
        PrintWriter pw = wrap(ctx.outputWriter());

        ConnectionPoolConfig c = getConnectionPoolConfig();
        List<List<Object>> data = new ArrayList<>();
        data.add(List.of("idleTimeout", c.idleTimeout));
        data.add(List.of("leakDetectionThreshold", c.leakDetectionThreshold));
        data.add(List.of("maximumPoolSize", c.maximumPoolSize));
        data.add(List.of("minimumIdle", c.minimumIdle));
        data.add(List.of("poolName", c.poolName));
        data.add(List.of("validationTimeout", c.validationTimeout));
        data.add(List.of("connectionTimeout", c.connectionTimeout));

        pw.println(ShellSupport.prettyPrint(
                new ListTableModel<>(data,
                        List.of("Name", "Value"),
                        List::get)));

    }

    @Command(value = "Show connection pool status",
            name = {"show", "pool", "status"},
            alias = {"sps"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.CONFIG_COMMANDS)
    public void showPoolStatus(CommandContext ctx) {
        PrintWriter pw = wrap(ctx.outputWriter());

        ConnectionPoolState c = getConnectionPoolSize();
        List<List<Object>> data = new ArrayList<>();
        data.add(List.of("activeConnections", c.activeConnections));
        data.add(List.of("idleConnections", c.idleConnections));
        data.add(List.of("totalConnections", c.totalConnections));
        data.add(List.of("threadsAwaitingConnection", c.threadsAwaitingConnection));

        pw.println(ShellSupport.prettyPrint(
                new ListTableModel<>(data,
                        List.of("Name", "Value"),
                        List::get)));
    }

    @Command(value = "Show effective model YAML",
            name = {"show", "model"},
            alias = {"sm"},
            group = CommandGroups.CONFIG_COMMANDS,
            exitStatusExceptionMapper = "commandExceptionMapper")
    public void showEffectiveModel(CommandContext ctx) {
        wrap(ctx.outputWriter())
                .printf("#(bright_white)%s#(default)%n",
                        yamlObjectMapper.writerFor(Root.class)
                                .writeValueAsString(new Root(batteryModel)));
    }

    @Command(value = "Validate model YAML",
            name = {"validate", "model"},
            alias = {"vm"},
            group = CommandGroups.CONFIG_COMMANDS,
            exitStatusExceptionMapper = "commandExceptionMapper")
    public void validateModel(CommandContext ctx) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<BatteryModel>> violations = validator.validate(batteryModel);

            PrintWriter pw = wrap(ctx.outputWriter());
            if (!violations.isEmpty()) {
                pw.printf("#(bright_red)%s#(default)%n",
                        "There are errors! " + AsciiArt.flipTableRoughly());
                violations.forEach(v -> {
                    pw.println(v.getMessage());
                });
            } else {
                pw.println("All good! " + AsciiArt.shrug());
            }
        }
    }
}
