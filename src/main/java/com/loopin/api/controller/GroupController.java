package com.loopin.api.controller;

import com.loopin.api.dto.group.request.CreateGroupRequest;
import com.loopin.api.dto.group.request.UpdateGroupRequest;
import com.loopin.api.dto.group.request.UpdateGroupStatusRequest;
import com.loopin.api.dto.group.response.GroupResponse;
import com.loopin.api.service.implementation.GroupServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupServiceImpl groupService;

    public GroupController(GroupServiceImpl groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader,
            Authentication authentication) {
        String currentUsername = resolveUsername(authentication, emailHeader);
        GroupResponse response = groupService.createGroup(request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> addMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.addMember(groupId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader,
            Authentication authentication) {

        String currentUsername = resolveUsername(authentication, emailHeader);
        GroupResponse response = groupService.updateGroup(
                groupId,
                request,
                currentUsername
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{groupId}/status")
    public ResponseEntity<GroupResponse> updateGroupStatus(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupStatusRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader,
            Authentication authentication) {

        String currentUsername = resolveUsername(authentication, emailHeader);
        GroupResponse response = groupService.updateGroupStatus(
                groupId,
                request,
                currentUsername);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMember(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    private String resolveUsername(Authentication authentication, String emailHeader) {
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return (emailHeader != null && !emailHeader.isBlank()) ? emailHeader : "admin@email.com";
    }
}