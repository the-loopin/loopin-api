package com.loopin.api.seed;


import com.loopin.api.common.enums.RequestStatus;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupJoinRequest;
import com.loopin.api.entity.User;
import com.loopin.api.repository.GroupJoinRequestRepository;
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

        // Seed exactly 10 requests using various groups and users
        for (int i = 0; i < 10; i++) {
            GroupJoinRequest request = new GroupJoinRequest();

            EventGroup group = groups.get(i % groups.size());
            User candidate = pickNonAdminUser(group, users, (i + 2) % users.size());

            request.setGroup(group);
            request.setUser(candidate);
            request.setStatus(statuses[i % statuses.length]);
            request.setMessage("Hey! I am really interested in joining this group. Request sequence #" + (i + 1));

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
