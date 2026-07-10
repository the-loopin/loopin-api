package com.loopin.api.groups.controller;

import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupStatusRequest;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.changegroupstatus.ChangeGroupStatusCommand;
import com.loopin.api.groups.changegroupstatus.ChangeGroupStatusHandler;
import com.loopin.api.groups.creategroup.CreateGroupCommand;
import com.loopin.api.groups.creategroup.CreateGroupHandler;
import com.loopin.api.groups.getgroupdetails.GetGroupDetailsHandler;
import com.loopin.api.groups.getgroupdetails.GetGroupDetailsQuery;
import com.loopin.api.groups.updategroup.UpdateGroupCommand;
import com.loopin.api.groups.updategroup.UpdateGroupHandler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.loopin.api.common.security.SecurityUtils;

import java.util.UUID;

@RestController
@RequestMapping("/v1/groups")
public class GroupController {

    private final CreateGroupHandler createGroupHandler;
    private final GetGroupDetailsHandler getGroupDetailsHandler;
    private final UpdateGroupHandler updateGroupHandler;
    private final ChangeGroupStatusHandler changeGroupStatusHandler;

    public GroupController(CreateGroupHandler createGroupHandler, GetGroupDetailsHandler getGroupDetailsHandler,
                           UpdateGroupHandler updateGroupHandler, ChangeGroupStatusHandler changeGroupStatusHandler) {
        this.createGroupHandler = createGroupHandler;
        this.getGroupDetailsHandler = getGroupDetailsHandler;
        this.updateGroupHandler = updateGroupHandler;
        this.changeGroupStatusHandler = changeGroupStatusHandler;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request) {
        String currentUsername = SecurityUtils.getRequiredCurrentUserEmail();
        GroupResponse response = createGroupHandler.handle(new CreateGroupCommand(request, currentUsername));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(getGroupDetailsHandler.handle(new GetGroupDetailsQuery(groupId)));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request) {

        String currentUsername = SecurityUtils.getRequiredCurrentUserEmail();
        GroupResponse response = updateGroupHandler.handle(new UpdateGroupCommand(groupId, request, currentUsername));

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{groupId}/status")
    public ResponseEntity<GroupResponse> updateGroupStatus(
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupStatusRequest request) {

        String currentUsername = SecurityUtils.getRequiredCurrentUserEmail();
        GroupResponse response = changeGroupStatusHandler.handle(
                new ChangeGroupStatusCommand(groupId, request, currentUsername));

        return ResponseEntity.ok(response);
    }
}
