package com.loopin.api.groups.controller;

import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupStatusRequest;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.service.GroupServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.loopin.api.common.security.SecurityUtils;

import java.util.UUID;

@RestController
@RequestMapping("/v1/groups")
public class GroupController {

    private final GroupServiceImpl groupService;

    public GroupController(GroupServiceImpl groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request) {
        String currentUsername = SecurityUtils.getRequiredCurrentUserEmail();
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
            @Valid @RequestBody UpdateGroupRequest request) {

        String currentUsername = SecurityUtils.getRequiredCurrentUserEmail();
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
            @Valid @RequestBody UpdateGroupStatusRequest request) {

        String currentUsername = SecurityUtils.getRequiredCurrentUserEmail();
        GroupResponse response = groupService.updateGroupStatus(
                groupId,
                request,
                currentUsername);

        return ResponseEntity.ok(response);
    }
}
