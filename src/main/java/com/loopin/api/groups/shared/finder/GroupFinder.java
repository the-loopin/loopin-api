package com.loopin.api.groups.shared.finder;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GroupFinder {

    private final EventGroupRepository eventGroupRepository;
    private final UserRepository userRepository;

    public EventGroup findGroup(UUID groupId) {
        return eventGroupRepository.findByPublicId(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
    }

    public User findCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    public User findActiveUser(UUID userId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
