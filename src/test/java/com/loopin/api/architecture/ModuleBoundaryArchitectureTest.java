package com.loopin.api.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModuleBoundaryArchitectureTest {

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
