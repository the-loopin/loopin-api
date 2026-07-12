package com.loopin.api.groups.api;

import java.util.UUID;

/**
 * Groups-owned authorization boundary for subscriptions to a group's messages.
 */
public interface GroupSubscriptionAuthorizer {

    GroupSubscriptionAuthorization authorize(UUID groupPublicId, Long userId);
}
