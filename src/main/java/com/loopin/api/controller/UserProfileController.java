package com.loopin.api.controller;

import com.loopin.api.dto.UserProfileRequest;
import com.loopin.api.dto.UserProfileResponse;
import com.loopin.api.entity.UserProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.loopin.api.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
         Long currentUserId = 1L;

        UserProfileResponse response = profileService.getProfile(currentUserId);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(@Valid @RequestBody UserProfileRequest request) {
        Long currentUserId = 1L;

        UserProfileResponse response = profileService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/me/badges")
    public ResponseEntity<List<String>> getMyBadges() {
        Long currentUserId = 1L;
        return ResponseEntity.ok(List.of("EVENT_ATTENDEE", "GROUP_CREATOR"));
    }
}


