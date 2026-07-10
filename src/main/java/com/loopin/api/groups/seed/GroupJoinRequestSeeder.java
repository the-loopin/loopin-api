package com.loopin.api.groups.seed;


import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.users.entity.User;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GroupJoinRequestSeeder {

    private final GroupJoinRequestRepository groupJoinRequestRepository;

    public void seedRequests(List<EventGroup> groups, List<User> users) {
        if (groupJoinRequestRepository.count() > 0) {
            return;
        }

        List<GroupJoinRequest> requests = new ArrayList<>();
        RequestStatus[] statuses = {RequestStatus.PENDING, RequestStatus.ACCEPTED, RequestStatus.REJECTED};

        String[] requestMessages = {
            "Hi, I've been working with Kubernetes for a year and would love to join your group to discuss best practices.",
            "Hey! I'm a software developer looking to join a startup team. I'd love to chat with you guys at the Pitch Night.",
            "Hello! As an HR manager, I'm really interested in your remote work discussion. Let's connect!",
            "Hey there! I am developing a teaching assistant tool using GPT-4 and would love to exchange ideas in your AI group.",
            "Hi, I'm traveling to Shusha from Baku and would love to join your group to share a ride and coordinate.",
            "Hey guys, I'm aiming for a 50-minute 10K. Let's run together during the charity marathon!",
            "Hi! I'm bringing Settlers of Catan to the mixer. Would love to join your table and play a game.",
            "Hello, I want to practice my German and would love to join the language coffee exchange group.",
            "Hey, I am very interested in this DevOps summit group, looking to learn more about Docker scaling.",
            "Hi! I am working on a new fintech project and would love to brainstorm with you guys during the Pitch Night."
        };

        // Seed exactly 10 requests using various groups and users
        for (int i = 0; i < 10; i++) {
            GroupJoinRequest request = new GroupJoinRequest();

            EventGroup group = groups.get(i % groups.size());
            User candidate = pickNonAdminUser(group, users, (i + 2) % users.size());

            request.setGroup(group);
            request.setUser(candidate);
            request.setStatus(statuses[i % statuses.length]);
            request.setMessage(requestMessages[i]);

            requests.add(request);
        }

        groupJoinRequestRepository.saveAll(requests);
    }

    /**
     * Picks a user that is not the group's own admin, so we never seed a
     * request from a group admin to join their own group. Falls back by
     * scanning forward through the user list if the initial candidate
     * happens to be the admin.
     */
    private User pickNonAdminUser(EventGroup group, List<User> users, int startIndex) {
        for (int offset = 0; offset < users.size(); offset++) {
            User candidate = users.get((startIndex + offset) % users.size());
            if (!candidate.equals(group.getAdmin())) {
                return candidate;
            }
        }
        // All users are the admin (shouldn't happen with a real user list)
        return users.get(startIndex);
    }
}
