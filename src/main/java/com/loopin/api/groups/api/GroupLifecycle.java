package com.loopin.api.groups.api;

import com.loopin.api.groups.entity.EventGroup;
import java.util.List;

public interface GroupLifecycle {
    List<EventGroup> archiveActiveGroupsForEvent(Long eventId);
}
