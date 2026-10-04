package io.battery.shell;

import org.slf4j.LoggerFactory;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;

import ch.qos.logback.classic.Level;

import io.battery.config.DataSourceConfig;
import static io.battery.util.AnsiPrintWriter.wrap;

@ShellComponent
public class LoggingCommands extends AbstractShellCommand {
    private static boolean toggleTraceLogLevel(String name) {
        ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext)
                LoggerFactory.getILoggerFactory();
        if (loggerContext.getLogger(name).getLevel().isGreaterOrEqual(Level.DEBUG)) {
            loggerContext.getLogger(name).setLevel(Level.TRACE);
            return true;
        } else {
            loggerContext.getLogger(name).setLevel(Level.INFO);
            return false;
        }
    }

    private static Level setLogLevel(String name, Level logLevel) {
        ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext)
                LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(name).setLevel(logLevel);
        return logLevel;
    }

    @Command(value = "Toggle SQL trace logging",
            name = {"log", "sql"},
            alias = {"ls"},
            group = CommandGroups.LOGGING_COMMANDS)
    public void toggleSqlTraceLogging(CommandContext ctx) {
        boolean enabled = toggleTraceLogLevel(DataSourceConfig.SQL_TRACE_LOGGER);
        wrap(ctx.outputWriter())
                .printf("SQL Trace Logging #(bright_yellow)%s#(default)%n"
                        .formatted(enabled ? "ENABLED" : "DISABLED"));
    }

    @Command(value = "Set app log level to TRACE",
            name = {"log", "trace"},
            alias = {"lt"},
            group = CommandGroups.LOGGING_COMMANDS)
    public void setAppTraceLogging(CommandContext ctx) {
        wrap(ctx.outputWriter())
                .printf("App log level set to #(bright_yellow)%s#(default)%n"
                        .formatted(setLogLevel("io.battery", Level.TRACE)));
    }

    @Command(value = "Set app log level to DEBUG",
            name = {"log", "debug"},
            alias = {"ld"},
            group = CommandGroups.LOGGING_COMMANDS)
    public void setAppDebugLogging(CommandContext ctx) {
        wrap(ctx.outputWriter())
                .printf("App log level set to #(bright_yellow)%s#(default)%n"
                        .formatted(setLogLevel("io.battery", Level.DEBUG)));
    }

    @Command(value = "Set app log level to INFO",
            name = {"log", "info"},
            alias = {"li"},
            group = CommandGroups.LOGGING_COMMANDS)
    public void setAppInfoLogging(CommandContext ctx) {
        wrap(ctx.outputWriter())
                .printf("App log level set to #(bright_yellow)%s#(default)%n"
                        .formatted(setLogLevel("io.battery", Level.INFO)));
    }

}
