package com.loopin.api.groups.shared.policy;

import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupCapacityPolicyTest {

    private final GroupCapacityPolicy policy = new GroupCapacityPolicy();

    @Test
    void refreshStatus_transitionsBetweenOpenAndFullFromMemberCount() {
        EventGroup group = new EventGroup();
        group.setGroupSize(GroupSizeType.TWO);
        group.setStatus(GroupStatus.OPEN);
        policy.applyMaximumFromSize(group);

        assertTrue(policy.refreshStatus(group, 2));
        assertEquals(GroupStatus.FULL, group.getStatus());
        assertTrue(policy.refreshStatus(group, 1));
        assertEquals(GroupStatus.OPEN, group.getStatus());
    }

    @Test
    void refreshStatus_preservesTerminalLifecycleStatus() {
        EventGroup group = new EventGroup();
        group.setGroupSize(GroupSizeType.TWO);
        group.setStatus(GroupStatus.ARCHIVED);
        policy.applyMaximumFromSize(group);

        assertFalse(policy.refreshStatus(group, 0));
        assertEquals(GroupStatus.ARCHIVED, group.getStatus());
    }
}
