package com.loopin.api.groups.shared.finder;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.api.UserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GroupFinder {

    private final EventGroupRepository eventGroupRepository;
    private final UserLookup userLookup;

    public EventGroup findGroup(UUID groupId) {
        return eventGroupRepository.findByPublicId(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
    }

    public User findCurrentUser(String email) {
        return userLookup.findByEmail(email);
    }

    public User findActiveUser(UUID userId) {
        return userLookup.findActiveByPublicId(userId);
    }
}
