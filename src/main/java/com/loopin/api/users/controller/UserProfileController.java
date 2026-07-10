package com.loopin.api.users.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.interests.dto.InterestResponse;
import com.loopin.api.interests.dto.UpdateUserInterestsRequest;
import com.loopin.api.users.dto.profile.request.UpdateUserProfileRequest;
import com.loopin.api.users.dto.profile.response.UserProfileResponse;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        Long currentUserId = getCurrentUserId();

        UserProfileResponse response = profileService.getProfile(currentUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        Long currentUserId = getCurrentUserId();

        UserProfileResponse response = profileService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/interests")
    public ResponseEntity<List<InterestResponse>> getMyInterests() {
        Long currentUserId = getCurrentUserId();

        return ResponseEntity.ok(profileService.getInterests(currentUserId));
    }

    @PutMapping("/me/interests")
    public ResponseEntity<List<InterestResponse>> updateMyInterests(
            @Valid @RequestBody UpdateUserInterestsRequest request
    ) {
        Long currentUserId = getCurrentUserId();

        return ResponseEntity.ok(profileService.updateInterests(currentUserId, request));
    }

    @GetMapping("/me/badges")
    public ResponseEntity<List<String>> getMyBadges() {
        Long currentUserId = getCurrentUserId();

        return ResponseEntity.ok(profileService.getUserBadges(currentUserId));
    }

    private Long getCurrentUserId() {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication required."
                ));

        return currentUser.getId();
    }
}
