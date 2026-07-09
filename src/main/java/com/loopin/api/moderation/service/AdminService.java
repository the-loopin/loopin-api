package com.loopin.api.moderation.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.core.events.enums.EventStatus;
import com.loopin.api.moderation.dto.admin.response.DashboardStatsResponse;
import com.loopin.api.core.events.dto.response.EventResponse;
import com.loopin.api.core.users.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminService {

    DashboardStatsResponse getDashboardStats();

    Page<UserResponse> getUsers(Pageable pageable);

    UserResponse updateUserRole(UUID targetUserId, Role role, String currentAdminIdentifier);

    void deleteUser(UUID targetUserId, String currentAdminIdentifier);

    Page<EventResponse> getEvents(EventStatus status, Pageable pageable);

    void deleteEvent(UUID eventId, String currentAdminIdentifier);
}
