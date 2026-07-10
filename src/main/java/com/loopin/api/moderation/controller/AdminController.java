package com.loopin.api.moderation.controller;

import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.moderation.dto.admin.response.DashboardStatsResponse;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.users.dto.request.UpdateUserRoleRequest;
import com.loopin.api.users.dto.response.UserResponse;
import com.loopin.api.moderation.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getUsers(pageable));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        String adminIdentifier = resolveAdminIdentifier(authentication);
        UserResponse response = adminService.updateUserRole(id, request.getRole(), adminIdentifier);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String adminIdentifier = resolveAdminIdentifier(authentication);
        adminService.deleteUser(id, adminIdentifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events")
    public ResponseEntity<Page<EventResponse>> getEvents(
            @RequestParam(required = false) EventStatus status,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getEvents(status, pageable));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String adminIdentifier = resolveAdminIdentifier(authentication);
        adminService.deleteEvent(id, adminIdentifier);
        return ResponseEntity.noContent().build();
    }

    private String resolveAdminIdentifier(Authentication authentication) {
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return null;
    }
}
