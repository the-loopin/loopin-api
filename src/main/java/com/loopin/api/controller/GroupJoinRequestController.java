package com.loopin.api.controller;


import com.loopin.api.dto.group.request.GroupJoinRequestRequest;
import com.loopin.api.dto.group.response.GroupJoinRequestResponse;
import com.loopin.api.service.abstraction.GroupJoinRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/group-join-requests")
@RequiredArgsConstructor
public class GroupJoinRequestController {

    private final GroupJoinRequestService joinRequestService;

    @PostMapping
    public ResponseEntity<GroupJoinRequestResponse> create(@Valid @RequestBody GroupJoinRequestRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(joinRequestService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupJoinRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(joinRequestService.getById(id));
    }


    @GetMapping("/groups/{groupId}")
    public ResponseEntity<List<GroupJoinRequestResponse>> getByGroupId(@PathVariable Long groupId) {
        return ResponseEntity.ok(joinRequestService.getByGroupId(groupId));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<GroupJoinRequestResponse>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(joinRequestService.getByUserId(userId));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<GroupJoinRequestResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(joinRequestService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<GroupJoinRequestResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(joinRequestService.reject(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        joinRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}