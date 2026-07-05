package com.loopin.api.seed;

import com.loopin.api.common.enums.GroupSizeType;
import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.User;
import com.loopin.api.repository.EventGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventGroupSeeder {

    private final EventGroupRepository eventGroupRepository;
    private final UserSeeder userSeeder;

    public List<EventGroup> seedGroups(List<Event> events, List<User> users) {
        if (eventGroupRepository.count() > 0) {
            return eventGroupRepository.findAll();
        }

        // Group admins should be regular users, not the system admin.
        List<User> admins = userSeeder.getRegularUsers(users);
        if (admins.isEmpty()) {
            admins = users; // fallback, should not normally happen
        }

        List<EventGroup> groups = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            EventGroup group = new EventGroup();
            group.setEvent(events.get(i)); // 1 group per event to reach 8 groups
            group.setAdmin(admins.get(i % admins.size())); // Set group admin

            // Keep groupSize and maxMembers consistent with each other,
            // instead of letting maxMembers drift to arbitrary values.
            boolean isFixedFour = i % 2 == 0;
            GroupSizeType sizeType = isFixedFour ? GroupSizeType.FOUR : GroupSizeType.FOUR_PLUS;
            group.setGroupSize(sizeType);
            group.setMaxMembers(isFixedFour ? 4 : 4 + (i + 1)); // FOUR_PLUS groups vary but never below 4

            group.setTitle("Networking Synergy Squad " + (i + 1));
            group.setStatus(GroupStatus.OPEN);
            group.setGroupNote("Let's sync up, grab coffee, and share our insights regarding this event!");

            groups.add(group);
        }

        return eventGroupRepository.saveAll(groups);
    }
}
