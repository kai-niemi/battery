package io.battery.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Finds configuration and script files under a base directory: application YAML files, whose
 * names map to Spring profiles ({@code application-retail.yml} for the {@code retail} profile),
 * and Battery and SQL script files.
 */
public abstract class PathUtils {
    private PathUtils() {
    }

    public static List<Path> listApplicationFiles(Path baseDir, String glob)
            throws UncheckedIOException {
        if (!baseDir.toFile().isDirectory()) {
            return List.of();
        }

        final PathMatcher pathMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:**/" + glob);

        final List<Path> fileList = new ArrayList<>();

        try {
            Files.walkFileTree(baseDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    if (!Files.isDirectory(path) && pathMatcher.matches(path)) {
                        fileList.add(path);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return fileList;
    }

    public static List<Path> listApplicationYamlFiles(Path... baseDirs)
            throws UncheckedIOException {
        return Arrays.stream(baseDirs)
                .flatMap(path -> listApplicationFiles(path, "*.yml").stream())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<Path> listBatteryScriptFiles(Path baseDir)
            throws UncheckedIOException {
        return listApplicationFiles(baseDir, "*.{b,sql}");
    }

    public static List<String> toProfileNames(Collection<Path> paths) {
        return paths.stream()
                .map(Path::getFileName)
                .map(Objects::toString)
                .map(s -> s.replace("application-", ""))
                .map(s -> s.replace(".yml", ""))
                .collect(Collectors.toList());
    }
}
