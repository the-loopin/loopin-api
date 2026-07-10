package com.loopin.api.groups.controller;

import com.loopin.api.groups.dto.request.GroupMemberRequest;
import com.loopin.api.groups.dto.response.GroupMemberResponse;
import com.loopin.api.groups.addgroupmember.AddGroupMemberCommand;
import com.loopin.api.groups.addgroupmember.AddGroupMemberHandler;
import com.loopin.api.groups.getmembershipdetails.GetMembershipDetailsHandler;
import com.loopin.api.groups.getmembershipdetails.GetMembershipDetailsQuery;
import com.loopin.api.groups.listgroupmembers.ListGroupMembersHandler;
import com.loopin.api.groups.listgroupmembers.ListGroupMembersQuery;
import com.loopin.api.groups.removegroupmember.RemoveGroupMemberCommand;
import com.loopin.api.groups.removegroupmember.RemoveGroupMemberHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.loopin.api.common.security.SecurityUtils;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/groups/{groupId}/members")
@RequiredArgsConstructor
public class GroupMemberController {

    private final AddGroupMemberHandler addGroupMemberHandler;
    private final GetMembershipDetailsHandler getMembershipDetailsHandler;
    private final ListGroupMembersHandler listGroupMembersHandler;
    private final RemoveGroupMemberHandler removeGroupMemberHandler;

    @PostMapping
    public ResponseEntity<GroupMemberResponse> addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupMemberRequest dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addGroupMemberHandler.handle(
                        new AddGroupMemberCommand(groupId, dto.getUserId(), SecurityUtils.getRequiredCurrentUserEmail())));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GroupMemberResponse> getByUserId(
            @PathVariable UUID groupId,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(getMembershipDetailsHandler.handle(new GetMembershipDetailsQuery(groupId, userId)));
    }

    @GetMapping
    public ResponseEntity<List<GroupMemberResponse>> getAll(@PathVariable UUID groupId) {
        return ResponseEntity.ok(listGroupMembersHandler.handle(new ListGroupMembersQuery(groupId)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId
    ) {
        removeGroupMemberHandler.handle(
                new RemoveGroupMemberCommand(groupId, userId, SecurityUtils.getRequiredCurrentUserEmail()));
        return ResponseEntity.noContent().build();
    }
}
