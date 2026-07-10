package com.loopin.api.groups.controller;


import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.groups.dto.request.CreateGroupJoinRequestRequest;
import com.loopin.api.groups.dto.response.GroupJoinRequestResponse;
import com.loopin.api.groups.approvegroupjoinrequest.ApproveGroupJoinRequestCommand;
import com.loopin.api.groups.approvegroupjoinrequest.ApproveGroupJoinRequestHandler;
import com.loopin.api.groups.creategroupjoinrequest.CreateGroupJoinRequestCommand;
import com.loopin.api.groups.creategroupjoinrequest.CreateGroupJoinRequestHandler;
import com.loopin.api.groups.deletegroupjoinrequest.DeleteGroupJoinRequestCommand;
import com.loopin.api.groups.deletegroupjoinrequest.DeleteGroupJoinRequestHandler;
import com.loopin.api.groups.getgroupjoinrequest.GetGroupJoinRequestHandler;
import com.loopin.api.groups.getgroupjoinrequest.GetGroupJoinRequestQuery;
import com.loopin.api.groups.listgroupjoinrequests.ListGroupJoinRequestsHandler;
import com.loopin.api.groups.listgroupjoinrequests.ListGroupJoinRequestsQuery;
import com.loopin.api.groups.listmygroupjoinrequests.ListMyGroupJoinRequestsHandler;
import com.loopin.api.groups.listmygroupjoinrequests.ListMyGroupJoinRequestsQuery;
import com.loopin.api.groups.rejectgroupjoinrequest.RejectGroupJoinRequestCommand;
import com.loopin.api.groups.rejectgroupjoinrequest.RejectGroupJoinRequestHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class GroupJoinRequestController {

    private final CreateGroupJoinRequestHandler createGroupJoinRequestHandler;
    private final GetGroupJoinRequestHandler getGroupJoinRequestHandler;
    private final ListGroupJoinRequestsHandler listGroupJoinRequestsHandler;
    private final ListMyGroupJoinRequestsHandler listMyGroupJoinRequestsHandler;
    private final ApproveGroupJoinRequestHandler approveGroupJoinRequestHandler;
    private final RejectGroupJoinRequestHandler rejectGroupJoinRequestHandler;
    private final DeleteGroupJoinRequestHandler deleteGroupJoinRequestHandler;

    @PostMapping("/groups/{groupId}/join-requests")
    public ResponseEntity<GroupJoinRequestResponse> create(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateGroupJoinRequestRequest request
    ) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createGroupJoinRequestHandler.handle(new CreateGroupJoinRequestCommand(groupId, currentUserId, request)));
    }

    @GetMapping("/groups/{groupId}/join-requests/{requestId}")
    public ResponseEntity<GroupJoinRequestResponse> getById(
            @PathVariable UUID groupId,
            @PathVariable UUID requestId
    ) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(getGroupJoinRequestHandler.handle(
                new GetGroupJoinRequestQuery(groupId, requestId, currentUserId)));
    }


    @GetMapping("/groups/{groupId}/join-requests")
    public ResponseEntity<List<GroupJoinRequestResponse>> getByGroupId(@PathVariable UUID groupId) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(listGroupJoinRequestsHandler.handle(
                new ListGroupJoinRequestsQuery(groupId, currentUserId)));
    }

    @GetMapping("/me/group-join-requests")
    public ResponseEntity<List<GroupJoinRequestResponse>> getMyRequests() {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(listMyGroupJoinRequestsHandler.handle(new ListMyGroupJoinRequestsQuery(currentUserId)));
    }

    @PatchMapping("/groups/{groupId}/join-requests/{requestId}/approve")
    public ResponseEntity<GroupJoinRequestResponse> approve(
            @PathVariable UUID groupId,
            @PathVariable UUID requestId
    ) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(approveGroupJoinRequestHandler.handle(
                new ApproveGroupJoinRequestCommand(groupId, requestId, currentUserId)));
    }

    @PatchMapping("/groups/{groupId}/join-requests/{requestId}/reject")
    public ResponseEntity<GroupJoinRequestResponse> reject(
            @PathVariable UUID groupId,
            @PathVariable UUID requestId
    ) {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(rejectGroupJoinRequestHandler.handle(
                new RejectGroupJoinRequestCommand(groupId, requestId, currentUserId)));
    }

    @DeleteMapping("/groups/{groupId}/join-requests/{requestId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID groupId,
            @PathVariable UUID requestId
    ) {
        Long currentUserId = getCurrentUserId();
        deleteGroupJoinRequestHandler.handle(new DeleteGroupJoinRequestCommand(groupId, requestId, currentUserId));
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
