package com.loopin.api.moderation.controller;

import com.loopin.api.moderation.dto.request.RejectModerationRequest;
import com.loopin.api.moderation.dto.response.ModerationItemResponse;
import com.loopin.api.moderation.service.ModerationReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/moderation")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminModerationController {

    private final ModerationReviewService moderationReviewService;

    @GetMapping("/pending")
    public ResponseEntity<Page<ModerationItemResponse>> getPendingItems(
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(moderationReviewService.getPendingItems(pageable));
    }

    @PatchMapping("/events/{id}/approve")
    public ResponseEntity<ModerationItemResponse> approveEvent(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(moderationReviewService.approveEvent(id, authentication.getName()));
    }

    @PatchMapping("/events/{id}/reject")
    public ResponseEntity<ModerationItemResponse> rejectEvent(
            @PathVariable UUID id,
            @Valid @RequestBody RejectModerationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(moderationReviewService.rejectEvent(id, request.getReason(), authentication.getName()));
    }
}
