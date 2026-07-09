package com.loopin.api.moderation.enums;

/**
 * The outcome of a content moderation check, independent of an entity's
 * workflow-specific status.
 */
public enum ContentModerationStatus {
    APPROVED,
    PENDING_REVIEW,
    REJECTED
}
