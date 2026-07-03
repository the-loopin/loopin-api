package com.loopin.api.controller;

import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.service.abstraction.GroupMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups/{groupId}/members")
@RequiredArgsConstructor
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    @PostMapping
    public ResponseEntity<GroupMemberResponse> addMember(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupMemberRequest dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupMemberService.addMember(groupId, dto));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GroupMemberResponse> getByUserId(
            @PathVariable Long groupId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(groupMemberService.getByGroupIdAndUserId(groupId, userId));
    }

    @GetMapping
    public ResponseEntity<List<GroupMemberResponse>> getAll(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupMemberService.getByGroupId(groupId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupMemberService.removeMember(groupId, userId);
        return ResponseEntity.noContent().build();
    }

}
