package com.loopin.api.groups.shared.joinrequest;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GroupJoinRequestFinder {
    private final GroupFinder groupFinder;
    private final GroupJoinRequestRepository requestRepository;

    public EventGroup findGroup(UUID groupId) {
        return groupFinder.findGroup(groupId);
    }

    public GroupJoinRequest findRequest(UUID groupId, UUID requestId) {
        EventGroup group = findGroup(groupId);
        return requestRepository.findByPublicIdAndGroupId(requestId, group.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Group join request not found with id: " + requestId));
    }
}
