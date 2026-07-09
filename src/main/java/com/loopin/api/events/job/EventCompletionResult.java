package com.loopin.api.core.events.job;

public record EventCompletionResult(
        boolean completed,
        int archivedGroups) {
}
