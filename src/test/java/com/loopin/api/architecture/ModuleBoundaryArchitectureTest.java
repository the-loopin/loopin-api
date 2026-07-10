package com.loopin.api.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ModuleBoundaryArchitectureTest {

    @Test
    void eventRuntimeCodeDoesNotImportGroupsRepositories() throws IOException {
        try (var paths = Files.walk(Path.of("src/main/java/com/loopin/api/events"))) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("\\seed\\") && !path.toString().contains("/seed/"))
                    .toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("com.loopin.api.groups.repository"),
                        () -> path + " must use com.loopin.api.groups.api instead of a Groups repository");
            }
        }
    }
}
