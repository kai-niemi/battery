package io.battery.script;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import io.battery.script.function.DefaultJdbcFunctions;
import io.battery.script.function.DefaultGenerateFunctions;
import io.battery.script.function.JdbcFunctions;
import io.battery.script.function.LoggingFunctions;
import io.battery.script.function.EncodingFunctions;
import io.battery.script.function.GenerateFunctions;
import io.battery.script.function.NetworkFunctions;
import io.battery.script.function.StandardFunctions;
import io.battery.script.function.WgsFunctions;

/**
 * Main gateway for parsing, validating and executing battery scripts from strings or files.
 * Holds a registry of named external objects (such as the standard function namespaces)
 * that scripts can reference, and returns the global variables left after execution.
 */
public class BatteryScript {
    private final Map<String, Object> externalVars = new HashMap<>();

    public void registerStandardFunctions() {
        putExternalVariable(GenerateFunctions.NAMESPACE, new DefaultGenerateFunctions());
        putExternalVariable(NetworkFunctions.NAMESPACE, new NetworkFunctions() {});
        putExternalVariable(LoggingFunctions.NAMESPACE, new LoggingFunctions() {});
        putExternalVariable(StandardFunctions.NAMESPACE, new StandardFunctions() {});
        putExternalVariable(WgsFunctions.NAMESPACE, new WgsFunctions() {});
        putExternalVariable(EncodingFunctions.NAMESPACE, new EncodingFunctions() {});
    }

    public void registerStandardFunctions(DataSource dataSource) {
        this.registerStandardFunctions();
        putExternalVariable(JdbcFunctions.NAMESPACE, new DefaultJdbcFunctions(dataSource));
    }

    public void putExternalVariable(String name, Object ref) {
        externalVars.put(name, ref);
    }

    public void forEachExternalVariable(Consumer<String> callback) {
        externalVars.forEach((name, objRef) -> callback.accept(name));
    }

    public void forEachMethod(String name, Consumer<Method> callback) {
        Assert.state(externalVars.containsKey(name), () -> "No such external variable: " + name);

        if (externalVars.get(name) instanceof Class<?>) {
            Class<?> clazz = (Class<?>) externalVars.get(name);
            ReflectionUtils.doWithMethods(clazz, callback::accept);
        } else {
            ReflectionUtils.doWithMethods(externalVars.get(name).getClass(), callback::accept);
        }
    }

    public void validate(String script) throws BatteryScriptException {
        FailFastErrorStrategy errorStrategy = new FailFastErrorStrategy();

        BatteryLexer lexer = new BatteryLexer(CharStreams.fromString(terminate(script)));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorStrategy);

        BatteryParser parser = new BatteryParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(errorStrategy);
        parser.addErrorListener(errorStrategy);

        parser.script();
    }

    public Map<String, Object> execute(Path script) throws BatteryScriptException {
        return execute(script, Map.of());
    }

    public Map<String, Object> execute(Path script, Map<String, Object> initialState) throws BatteryScriptException {
        try {
            BatteryLexer lexer = new BatteryLexer(CharStreams.fromPath(script));

            FailFastErrorStrategy errorStrategy = new FailFastErrorStrategy();
            lexer.removeErrorListeners();
            lexer.addErrorListener(errorStrategy);

            return execute(lexer, initialState, errorStrategy);
        } catch (BatteryScriptException e) {
            throw new BatteryScriptException("[%s] %s"
                    .formatted(script.getFileName(), e.getMessage()), e.getCause(), e.getOffendingTokenOffset());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Map<String, Object> execute(String script) throws BatteryScriptException {
        return execute(script, Map.of());
    }

    public Map<String, Object> execute(String script,
                                       Map<String, Object> initialState) throws BatteryScriptException {

        BatteryLexer lexer = new BatteryLexer(CharStreams.fromString(terminate(script)));

        FailFastErrorStrategy errorStrategy = new FailFastErrorStrategy();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorStrategy);

        return execute(lexer, initialState, errorStrategy);
    }

    /**
     * Terminates the last statement if needed, so that a trailing semicolon is optional
     * for both validation and execution.
     */
    private static String terminate(String script) {
        return script.endsWith(";") ? script : script + ";";
    }

    private Map<String, Object> execute(BatteryLexer lexer,
                                        Map<String, Object> initialState,
                                        FailFastErrorStrategy errorStrategy) {
        BatteryParser parser = new BatteryParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(errorStrategy);
        parser.addErrorListener(errorStrategy);

        BatteryParserTreeVisitor parseTreeVisitor = new BatteryParserTreeVisitor(parser, initialState, externalVars);

        try {
            parseTreeVisitor.visit(parser.script());
            return parseTreeVisitor.getGlobalVariables();
        } catch (Exception e) {
            // Rethrow script exceptions as-is to retain the position of the offending token,
            // including those wrapped by fork completions
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof BatteryScriptException scriptException) {
                    throw scriptException;
                }
                cause = cause.getCause();
            }
            throw BatteryScriptException.from(e.getMessage(), parser, e);
        }
    }
}