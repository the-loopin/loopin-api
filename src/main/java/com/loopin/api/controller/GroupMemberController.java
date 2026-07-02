package com.loopin.api.controller;

import com.loopin.api.dto.group.request.GroupMemberRequest;
import com.loopin.api.dto.group.response.GroupMemberResponse;
import com.loopin.api.service.implementation.GroupMemberServiceImpl;
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

    private final GroupMemberServiceImpl groupMemberService;

    @PostMapping
    public ResponseEntity<GroupMemberResponse> addMember(@Valid @RequestBody GroupMemberRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupMemberService.addMember(dto));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GroupMemberResponse> getById(@PathVariable Long userId) {
        return ResponseEntity.ok(groupMemberService.getById(userId));
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