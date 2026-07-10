package com.loopin.api.groups.shared.policy;

import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.enums.GroupStatus;
import org.springframework.stereotype.Component;

@Component
public class GroupStatusPolicy {

    public void changeStatus(EventGroup group, GroupStatus requestedStatus, int memberCount,
                             GroupCapacityPolicy capacityPolicy) {
        if ((group.getStatus() == GroupStatus.ARCHIVED || group.getStatus() == GroupStatus.CANCELLED)
                && group.getStatus() != requestedStatus) {
            throw new InvalidGroupStateException("A " + group.getStatus() + " group cannot be reopened");
        }
        if (requestedStatus == GroupStatus.OPEN || requestedStatus == GroupStatus.FULL) {
            capacityPolicy.refreshStatus(group, memberCount);
            return;
        }
        group.setStatus(requestedStatus);
    }
}
