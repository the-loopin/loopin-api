package com.loopin.api.groups.shared.policy;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupAdminPolicyTest {

    @Test
    void requiresTheConfiguredGroupAdmin() {
        User admin = new User("admin@loopin.test", "Admin", null);
        admin.setId(1L);
        EventGroup group = new EventGroup();
        group.setAdmin(admin);
        GroupAdminPolicy policy = new GroupAdminPolicy();

        assertDoesNotThrow(() -> policy.requireAdmin(group, "admin@loopin.test"));
        assertDoesNotThrow(() -> policy.requireAdmin(group, 1L));
        assertThrows(ForbiddenAccessException.class, () -> policy.requireAdmin(group, "other@loopin.test"));
    }
}
