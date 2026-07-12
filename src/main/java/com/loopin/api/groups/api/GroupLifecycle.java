package com.loopin.api.groups.api;

import java.util.List;

public interface GroupLifecycle {
    List<ArchivedGroupAwardRecipients> archiveActiveGroupsForEvent(Long eventId);
}
