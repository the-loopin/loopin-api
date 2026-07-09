package com.loopin.api.moderation;

import com.loopin.api.common.enums.ContentModerationStatus;

import java.util.List;

public record ContentModerationDecision(
        ContentModerationStatus status,
        List<String> matchedTerms
) {
    public boolean isApproved() {
        return status == ContentModerationStatus.APPROVED;
    }

    public static ContentModerationDecision approved() {
        return new ContentModerationDecision(ContentModerationStatus.APPROVED, List.of());
    }
}
