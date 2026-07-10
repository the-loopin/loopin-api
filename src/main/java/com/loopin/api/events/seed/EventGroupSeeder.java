package com.loopin.api.core.events.seed;

import com.loopin.api.core.groups.enums.GroupSizeType;
import com.loopin.api.core.groups.enums.GroupStatus;
import com.loopin.api.core.events.entity.Event;
import com.loopin.api.core.events.entity.EventGroup;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.core.events.repository.EventGroupRepository;
import com.loopin.api.core.users.seed.UserSeeder;
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

        String[] groupTitles = {
            "Kubernetes & DevOps Talk",
            "Pitch & Funding Circle",
            "HR Future & Remote Work",
            "AI in Classroom Chat",
            "Shusha Explorers Club",
            "Runners & Pacers Support",
            "Tabletop & Strategy Talk",
            "Language Practice & Cafe Chat"
        };

        String[] groupNotes = {
            "Discussing Kubernetes setups, CI/CD pipelines, and cloud provider experiences. Let's sync up near the registration desk!",
            "Connect with other startup founders and local investors after the pitch. Share your feedback on the pitches.",
            "Let's share our remote work experiences, employee engagement strategies, and tools for hybrid team alignment.",
            "For educators and developers working on or interested in using LLMs / generative AI for educational platforms.",
            "Coordination group for everyone joining the tour. Let's organize rides, shares, and track photo albums here!",
            "Discussing pacing, training plans, and race day logistics for the charity marathon. All paces are welcome!",
            "Coordinating what board games to bring and play during the mixer. Let us know what you want to play!",
            "A casual table for practicing English, German, Russian, and Azerbaijani. Let's grab some coffee and chat."
        };

        for (int i = 0; i < 8; i++) {
            EventGroup group = new EventGroup();
            group.setEvent(events.get(i)); // 1 group per event to reach 8 groups
            group.setAdmin(admins.get(i % admins.size())); // Set group admin

            // Keep groupSize and maxMembers consistent with each other
            boolean isFixedFour = i % 2 == 0;
            GroupSizeType sizeType = isFixedFour ? GroupSizeType.FOUR : GroupSizeType.FOUR_PLUS;
            group.setGroupSize(sizeType);
            group.setMaxMembers(isFixedFour ? 4 : 8 + i); 

            group.setTitle(groupTitles[i]);
            group.setStatus(GroupStatus.OPEN);
            group.setGroupNote(groupNotes[i]);

            groups.add(group);
        }

        return eventGroupRepository.saveAll(groups);
    }
}
