package com.loopin.api.groups.api;

import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class GroupSubscriptionAuthorizerService implements GroupSubscriptionAuthorizer {

    private final EventGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    @Override
    public GroupSubscriptionAuthorization authorize(UUID groupPublicId, Long userId) {
        Optional<EventGroup> group = groupRepository.findByPublicId(groupPublicId);
        if (group.isEmpty()) {
            return GroupSubscriptionAuthorization.GROUP_NOT_FOUND;
        }

        return memberRepository.existsByGroupIdAndUserId(group.get().getId(), userId)
                ? GroupSubscriptionAuthorization.ALLOWED
                : GroupSubscriptionAuthorization.NOT_A_MEMBER;
    }
}
