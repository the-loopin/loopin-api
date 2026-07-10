package com.loopin.api.groups.shared.policy;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.groups.entity.EventGroup;
import org.springframework.stereotype.Component;

@Component
public class GroupAdminPolicy {

    public void requireAdmin(EventGroup group, String email) {
        if (email == null || email.isBlank() || group.getAdmin() == null
                || !email.equals(group.getAdmin().getEmail())) {
            throw new ForbiddenAccessException("Only the group admin can manage this group");
        }
    }

    public void requireAdmin(EventGroup group, Long userId) {
        if (userId == null || group.getAdmin() == null || !userId.equals(group.getAdmin().getId())) {
            throw new ForbiddenAccessException("Only the group admin can manage this group");
        }
    }
}
