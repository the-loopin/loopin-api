package com.loopin.api.groups.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.groups.changegroupstatus.ChangeGroupStatusCommand;
import com.loopin.api.groups.changegroupstatus.ChangeGroupStatusHandler;
import com.loopin.api.groups.creategroup.CreateGroupCommand;
import com.loopin.api.groups.creategroup.CreateGroupHandler;
import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupImageRequest;
import com.loopin.api.groups.dto.request.UpdateGroupRequest;
import com.loopin.api.groups.dto.request.UpdateGroupStatusRequest;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.getgroupdetails.GetGroupDetailsHandler;
import com.loopin.api.groups.getgroupdetails.GetGroupDetailsQuery;
import com.loopin.api.groups.removegroupimage.RemoveGroupImageCommand;
import com.loopin.api.groups.removegroupimage.RemoveGroupImageHandler;
import com.loopin.api.groups.updategroup.UpdateGroupCommand;
import com.loopin.api.groups.updategroup.UpdateGroupHandler;
import com.loopin.api.groups.updategroupimage.UpdateGroupImageCommand;
import com.loopin.api.groups.updategroupimage.UpdateGroupImageHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final CreateGroupHandler createGroupHandler;
    private final GetGroupDetailsHandler getGroupDetailsHandler;
    private final UpdateGroupHandler updateGroupHandler;
    private final ChangeGroupStatusHandler changeGroupStatusHandler;
    private final UpdateGroupImageHandler updateGroupImageHandler;
    private final RemoveGroupImageHandler removeGroupImageHandler;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
        @Valid
        @RequestBody
        CreateGroupRequest request
    ) {
        String currentUsername =
            SecurityUtils.getRequiredCurrentUserEmail();

        GroupResponse response =
            createGroupHandler.handle(
                new CreateGroupCommand(
                    request,
                    currentUsername
                )
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(
        @PathVariable
        UUID groupId
    ) {
        return ResponseEntity.ok(
            getGroupDetailsHandler.handle(
                new GetGroupDetailsQuery(groupId)
            )
        );
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponse> updateGroup(
        @PathVariable
        UUID groupId,
        @Valid
        @RequestBody
        UpdateGroupRequest request
    ) {
        String currentUsername =
            SecurityUtils.getRequiredCurrentUserEmail();

        GroupResponse response =
            updateGroupHandler.handle(
                new UpdateGroupCommand(
                    groupId,
                    request,
                    currentUsername
                )
            );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{groupId}/image")
    public ResponseEntity<GroupResponse> updateGroupImage(
        @PathVariable
        UUID groupId,
        @Valid
        @RequestBody
        UpdateGroupImageRequest request
    ) {
        String currentUsername =
            SecurityUtils.getRequiredCurrentUserEmail();

        GroupResponse response =
            updateGroupImageHandler.handle(
                new UpdateGroupImageCommand(
                    groupId,
                    request.mediaId(),
                    currentUsername
                )
            );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}/image")
    public ResponseEntity<GroupResponse> removeGroupImage(
        @PathVariable
        UUID groupId
    ) {
        String currentUsername =
            SecurityUtils.getRequiredCurrentUserEmail();

        GroupResponse response =
            removeGroupImageHandler.handle(
                new RemoveGroupImageCommand(
                    groupId,
                    currentUsername
                )
            );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{groupId}/status")
    public ResponseEntity<GroupResponse>
    updateGroupStatus(
        @PathVariable
        UUID groupId,
        @Valid
        @RequestBody
        UpdateGroupStatusRequest request
    ) {
        String currentUsername =
            SecurityUtils.getRequiredCurrentUserEmail();

        GroupResponse response =
            changeGroupStatusHandler.handle(
                new ChangeGroupStatusCommand(
                    groupId,
                    request,
                    currentUsername
                )
            );

        return ResponseEntity.ok(response);
    }
}
