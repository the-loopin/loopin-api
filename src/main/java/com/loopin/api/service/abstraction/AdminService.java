package com.loopin.api.service.abstraction;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.dto.admin.response.DashboardStatsResponse;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.dto.user.response.UserResponse;
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
