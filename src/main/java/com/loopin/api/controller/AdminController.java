package com.loopin.api.controller;

import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.dto.admin.response.DashboardStatsResponse;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.dto.user.request.UpdateUserRoleRequest;
import com.loopin.api.dto.user.response.UserResponse;
import com.loopin.api.service.abstraction.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/admin", "/api/admin"})
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
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String adminIdentifier = resolveAdminIdentifier(authentication, userIdHeader);
        UserResponse response = adminService.updateUserRole(id, request.getRole(), adminIdentifier);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String adminIdentifier = resolveAdminIdentifier(authentication, userIdHeader);
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
            @PathVariable Long id,
            Authentication authentication,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String adminIdentifier = resolveAdminIdentifier(authentication, userIdHeader);
        adminService.deleteEvent(id, adminIdentifier);
        return ResponseEntity.noContent().build();
    }

    private String resolveAdminIdentifier(Authentication authentication, String userIdHeader) {
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return (userIdHeader != null && !userIdHeader.isBlank()) ? userIdHeader : null;
    }
}
