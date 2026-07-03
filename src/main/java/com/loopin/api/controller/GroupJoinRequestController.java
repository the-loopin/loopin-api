package com.loopin.api.controller;


import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.dto.group.request.CreateGroupJoinRequestRequest;
import com.loopin.api.dto.group.response.GroupJoinRequestResponse;
import com.loopin.api.service.abstraction.GroupJoinRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class GroupJoinRequestController {

    private final GroupJoinRequestService joinRequestService;

    @PostMapping("/groups/{groupId}/join-requests")
    public ResponseEntity<GroupJoinRequestResponse> create(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupJoinRequestRequest request
    ) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(joinRequestService.create(groupId, currentUserId, request));
    }

    @GetMapping("/groups/{groupId}/join-requests/{requestId}")
    public ResponseEntity<GroupJoinRequestResponse> getById(
            @PathVariable Long groupId,
            @PathVariable Long requestId
    ) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(joinRequestService.getById(groupId, requestId, currentUserId));
    }


    @GetMapping("/groups/{groupId}/join-requests")
    public ResponseEntity<List<GroupJoinRequestResponse>> getByGroupId(@PathVariable Long groupId) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(joinRequestService.getByGroupId(groupId, currentUserId));
    }

    @GetMapping("/me/group-join-requests")
    public ResponseEntity<List<GroupJoinRequestResponse>> getMyRequests() {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(joinRequestService.getByUserId(currentUserId));
    }

    @PatchMapping("/groups/{groupId}/join-requests/{requestId}/approve")
    public ResponseEntity<GroupJoinRequestResponse> approve(
            @PathVariable Long groupId,
            @PathVariable Long requestId
    ) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(joinRequestService.approve(groupId, requestId, currentUserId));
    }

    @PatchMapping("/groups/{groupId}/join-requests/{requestId}/reject")
    public ResponseEntity<GroupJoinRequestResponse> reject(
            @PathVariable Long groupId,
            @PathVariable Long requestId
    ) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(joinRequestService.reject(groupId, requestId, currentUserId));
    }

    @DeleteMapping("/groups/{groupId}/join-requests/{requestId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long groupId,
            @PathVariable Long requestId
    ) {
        Long currentUserId = getCurrentUserId();
        joinRequestService.delete(groupId, requestId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
