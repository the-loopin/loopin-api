package com.loopin.api.groups.shared.policy;

import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.enums.GroupStatus;
import org.springframework.stereotype.Component;

@Component
public class GroupMembershipPolicy {

    public void requireMembershipChangesAllowed(EventGroup group) {
        if (group.getStatus() == GroupStatus.ARCHIVED || group.getStatus() == GroupStatus.CANCELLED) {
            throw new InvalidGroupStateException(
                    "Group is " + group.getStatus() + " and no longer accepts membership updates");
        }
    }

    public void requireCanAddMember(EventGroup group, int memberCount) {
        requireMembershipChangesAllowed(group);
        if (group.getStatus() == GroupStatus.FULL || memberCount >= group.getMaxMembers()) {
            throw new InvalidGroupStateException("Group has reached its maximum number of members");
        }
    }

    public void requireNotMember(boolean alreadyMember) {
        if (alreadyMember) {
            throw new InvalidGroupStateException("User is already a member of this group");
        }
    }
}
