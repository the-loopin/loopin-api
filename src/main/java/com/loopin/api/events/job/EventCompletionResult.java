package com.loopin.api.events.job;

public record EventCompletionResult(
        boolean completed,
        int archivedGroups) {
}
