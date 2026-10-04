package io.battery.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class PathUtilsTest {
    @Test
    public void testListApplicationFilesAndProfiles(@TempDir Path tempDir) throws IOException {
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Path yml1 = Files.createFile(tempDir.resolve("application-dev.yml"));
        Path yml2 = Files.createFile(subDir.resolve("application-prod.yml"));
        Path scriptB = Files.createFile(tempDir.resolve("test.b"));
        Path scriptSql = Files.createFile(subDir.resolve("query.sql"));
        Path textFile = Files.createFile(tempDir.resolve("other.txt"));

        List<Path> ymlFiles = PathUtils.listApplicationFiles(tempDir, "*.yml");
        assertThat(ymlFiles).hasSize(2).contains(yml1, yml2);

        List<Path> nonExistent = PathUtils.listApplicationFiles(tempDir.resolve("non-existent-dir"), "*.yml");
        assertThat(nonExistent).isEmpty();

        List<Path> yamlFiles = PathUtils.listApplicationYamlFiles(tempDir, subDir);
        assertThat(yamlFiles).contains(yml1, yml2);

        List<Path> batteryScripts = PathUtils.listBatteryScriptFiles(tempDir);
        assertThat(batteryScripts).hasSize(2).contains(scriptB, scriptSql);

        List<String> profiles = PathUtils.toProfileNames(List.of(yml1, yml2));
        assertThat(profiles).isEqualTo(List.of("dev", "prod"));
    }
}
