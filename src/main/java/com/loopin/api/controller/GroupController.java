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
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

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
            Authentication authentication) {
        String currentUsername = resolveUsername(authentication);
        GroupResponse response = groupService.createGroup(request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request,
            Authentication authentication) {

        String currentUsername = resolveUsername(authentication);
        GroupResponse response = groupService.updateGroup(
                groupId,
                request,
                currentUsername
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{groupId}/status")
    public ResponseEntity<GroupResponse> updateGroupStatus(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupStatusRequest request,
            Authentication authentication) {

        String currentUsername = resolveUsername(authentication);
        GroupResponse response = groupService.updateGroupStatus(
                groupId,
                request,
                currentUsername);

        return ResponseEntity.ok(response);
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
