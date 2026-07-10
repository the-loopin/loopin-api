package com.loopin.api.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        try (var paths = Files.walk(Path.of("src/main/java/com/loopin/api/groups"))) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("\\seed\\") && !path.toString().contains("/seed/"))
                    .toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("com.loopin.api.notifications.service.NotificationService"),
                        () -> path + " must use com.loopin.api.notifications.api instead of notifications.service");
            }
        }
    }

    @Test
    void controllersDoNotImportRepositories() throws IOException {
        for (Path controllerDirectory : List.of(
                Path.of("src/main/java/com/loopin/api/events/controller"),
                Path.of("src/main/java/com/loopin/api/groups/controller"))) {
            try (var paths = Files.walk(controllerDirectory)) {
                for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertFalse(Files.readString(path).contains(".repository."),
                            () -> path + " must delegate to a handler rather than a repository");
                }
            }
        }
    }

    @Test
    void queryHandlersDeclareReadOnlyTransactionsAndCommandsDeclareWriteTransactions() throws IOException {
        assertTransactionMode("src/main/java/com/loopin/api/events", "get|list", "@Transactional(readOnly = true)");
        assertTransactionMode("src/main/java/com/loopin/api/groups", "get|list", "@Transactional(readOnly = true)");
        assertTransactionMode("src/main/java/com/loopin/api/events", "create|update|cancel|delete", "@Transactional");
        assertTransactionMode("src/main/java/com/loopin/api/groups",
                "create|update|change|add|remove|approve|reject|delete", "@Transactional");
    }

    @Test
    void brunoCollectionMetadataAndCoreRequestsExist() throws IOException {
        String metadata = Files.readString(Path.of("api-tests/bruno/bruno.json"));
        assertFalse(metadata.isBlank());
        assertFalse(!metadata.contains("\"type\": \"collection\""));
        assertFalse(!Files.exists(Path.of("api-tests/bruno/Events/List Published Events.bru")));
        assertFalse(!Files.exists(Path.of("api-tests/bruno/Groups/Create Group.bru")));
        assertFalse(!Files.exists(Path.of("api-tests/bruno/Join Requests/Create Join Request.bru")));
    }

    private void assertTransactionMode(String root, String packagePrefix, String expected) throws IOException {
        try (var paths = Files.walk(Path.of(root))) {
            for (Path path : paths.filter(path -> path.toString().endsWith("Handler.java"))
                    .filter(path -> path.getParent().getFileName().toString().matches(packagePrefix + ".*"))
                    .toList()) {
                assertFalse(!Files.readString(path).contains(expected),
                        () -> path + " must declare " + expected);
            }
        }
    }
}
