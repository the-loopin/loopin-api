package com.loopin.api.groups.api;

/**
 * The deterministic outcome of a group message subscription authorization check.
 */
public enum GroupSubscriptionAuthorization {
    ALLOWED,
    GROUP_NOT_FOUND,
    NOT_A_MEMBER
}
