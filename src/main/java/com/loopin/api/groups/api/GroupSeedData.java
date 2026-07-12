package com.loopin.api.groups.api;

import com.loopin.api.events.entity.Event;
import com.loopin.api.users.entity.User;

import java.util.List;

/** Groups-owned boundary for development data seeding. */
public interface GroupSeedData {

    int seedGroupsAndRequests(List<Event> events, List<User> users);
}
