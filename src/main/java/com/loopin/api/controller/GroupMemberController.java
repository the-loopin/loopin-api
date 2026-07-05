package com.loopin.api.controller;

import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.service.abstraction.GroupMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups/{groupId}/members")
@RequiredArgsConstructor
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    @PostMapping
    public ResponseEntity<GroupMemberResponse> addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupMemberRequest dto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupMemberService.addMember(groupId, dto, resolveUsername(authentication)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GroupMemberResponse> getByUserId(
            @PathVariable UUID groupId,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(groupMemberService.getByGroupIdAndUserId(groupId, userId));
    }

    @GetMapping
    public ResponseEntity<List<GroupMemberResponse>> getAll(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupMemberService.getByGroupId(groupId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        groupMemberService.removeMember(groupId, userId, resolveUsername(authentication));
        return ResponseEntity.noContent().build();
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
