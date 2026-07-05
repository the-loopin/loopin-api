package com.loopin.api.job;

public record EventCompletionResult(
        boolean completed,
        int archivedGroups) {
}
