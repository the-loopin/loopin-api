package com.loopin.api.groups.shared.policy;

import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import org.springframework.stereotype.Component;

@Component
public class GroupCapacityPolicy {

    public void applyMaximumFromSize(EventGroup group) {
        GroupSizeType groupSize = group.getGroupSize();
        if (groupSize == null) {
            throw new InvalidGroupStateException("Group size is required");
        }
        group.setMaxMembers(groupSize.getMaxMembers());
    }

    public void requireCapacityNotBelowMemberCount(EventGroup group, int memberCount) {
        if (group.getMaxMembers() < memberCount) {
            throw new InvalidGroupStateException(
                    "Max members cannot be less than the current member count: " + memberCount);
        }
    }

    public boolean refreshStatus(EventGroup group, int memberCount) {
        if (group.getStatus() == GroupStatus.ARCHIVED || group.getStatus() == GroupStatus.CANCELLED) {
            return false;
        }
        GroupStatus expected = memberCount >= group.getMaxMembers() ? GroupStatus.FULL : GroupStatus.OPEN;
        if (group.getStatus() == expected) {
            return false;
        }
        group.setStatus(expected);
        return true;
    }
}
