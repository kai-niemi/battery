package io.battery.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;

import io.battery.VariableSource;
import io.battery.script.foo.FooBarFunctions;
import io.battery.script.function.JdbcFunctions;

@Tag("unit-test")
public class ScriptFilesTest {
    private static final Predicate<Path> PATH_PREDICATE = path -> true;

    public static Stream<Arguments> scriptFilePaths() {
        List<Arguments> arguments = new ArrayList<>();
        try {
            try (Stream<Path> s = Files.list(Path.of("src/test/resources/scripts")).filter(PATH_PREDICATE)) {
                s.forEach(path -> arguments.add(Arguments.of(path)));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return arguments.stream();
    }

    public static JdbcFunctions jdbcFunctionsMock() {
        JdbcFunctions jdbcFunctionsMock = Mockito.mock(JdbcFunctions.class);

        Mockito.when(jdbcFunctionsMock.queryForList(Mockito.anyString()))
                .thenReturn(List.of(
                        Map.of(
                                "a", 1,
                                "b", 2,
                                "c", 3,
                                "d", 4,
                                "e", 5
                        ), Map.of(
                                "a", 6,
                                "b", 7,
                                "c", 8,
                                "d", 9,
                                "e", 10
                        )
                ));

        Mockito.when(jdbcFunctionsMock.queryForObject(Mockito.anyString(), Mockito.any(Object[].class)))
                .thenReturn(List.of(
                        Map.of(
                                "a", 1,
                                "b", 2,
                                "c", 3,
                                "d", 4,
                                "e", 5
                        )
                ));

        Mockito.when(jdbcFunctionsMock.queryForObject(Mockito.eq("select gen_random_uuid()")))
                .thenReturn(UUID.randomUUID());

        Mockito.when(jdbcFunctionsMock.queryForObject(Mockito.eq("select gen_random_uuid()")))
                .thenReturn(UUID.randomUUID());

        return jdbcFunctionsMock;
    }

    public static final Stream<Arguments> scriptFiles = scriptFilePaths();

    @ParameterizedTest
    @VariableSource("scriptFiles")
    public void givenScriptFile_whenEvaluated_expectNoErrors(Path path) {
        BatteryScript batteryScript = new BatteryScript();
        batteryScript.registerStandardFunctions();
        batteryScript.putExternalVariable(JdbcFunctions.NAMESPACE, jdbcFunctionsMock());
        batteryScript.putExternalVariable(FooBarFunctions.NAMESPACE, new FooBarFunctions());
        batteryScript.execute(path, Map.of());
    }
}
