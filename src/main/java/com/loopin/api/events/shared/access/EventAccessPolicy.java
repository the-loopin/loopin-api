package com.loopin.api.events.shared.access;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.events.entity.Event;
import com.loopin.api.users.entity.User;
import org.springframework.stereotype.Component;

@Component
public class EventAccessPolicy {

    public void requireOwnerOrAdmin(Event event, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (event.getOwner() != null && event.getOwner().getId().equals(currentUser.getId())) {
            return;
        }
        throw new ForbiddenAccessException("Only the event owner or an admin can modify this event");
    }
}
