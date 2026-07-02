package com.loopin.api.controller;

import com.loopin.api.common.security.SecurityContext;
import com.loopin.api.dto.user.request.UpdateUserRoleRequest;
import com.loopin.api.dto.user.request.UserRegisterRequest;
import com.loopin.api.dto.user.response.UserResponse;
import com.loopin.api.service.abstraction.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityContext securityContext;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegisterRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        securityContext.requireAdmin(roleHeader);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        securityContext.requireAdmin(roleHeader);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        
        if (userIdHeader != null) {
            securityContext.requireAuthenticatedUser(userIdHeader);
            return ResponseEntity.ok(userService.getUserById(userIdHeader));
        }

        org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Access denied. Authentication required.");
        }
        
        return ResponseEntity.ok(userService.getUserByEmail(authentication.getName()));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        securityContext.requireAdmin(roleHeader);
        return ResponseEntity.ok(userService.updateUserRole(id, request.getRole()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        securityContext.requireAdmin(roleHeader);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
