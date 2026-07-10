package com.loopin.api.groups.changegroupstatus;

import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.groups.shared.policy.GroupCapacityPolicy;
import com.loopin.api.groups.shared.policy.GroupStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChangeGroupStatusHandler {
    private final EventGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupMapper groupMapper;
    private final GroupFinder groupFinder;
    private final GroupAdminPolicy adminPolicy;
    private final GroupStatusPolicy statusPolicy;
    private final GroupCapacityPolicy capacityPolicy;

    @Transactional
    public GroupResponse handle(ChangeGroupStatusCommand command) {
        EventGroup group = groupFinder.findGroup(command.groupId());
        adminPolicy.requireAdmin(group, command.currentUsername());
        statusPolicy.changeStatus(group, command.request().getStatus(), memberRepository.countByGroupId(group.getId()), capacityPolicy);
        return groupMapper.toGroupResponse(groupRepository.save(group));
    }
}
