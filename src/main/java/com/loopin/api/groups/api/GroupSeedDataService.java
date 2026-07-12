package com.loopin.api.groups.api;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.seed.EventGroupSeeder;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.seed.GroupJoinRequestSeeder;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class GroupSeedDataService implements GroupSeedData {

    private final EventGroupSeeder eventGroupSeeder;
    private final GroupJoinRequestSeeder groupJoinRequestSeeder;

    @Override
    public int seedGroupsAndRequests(List<Event> events, List<User> users) {
        List<EventGroup> groups = eventGroupSeeder.seedGroups(events, users);
        groupJoinRequestSeeder.seedRequests(groups, users);
        return groups.size();
    }
}
