package com.loopin.api.core.users.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.core.users.dto.request.UserRegisterRequest;
import com.loopin.api.core.users.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse registerUser(UserRegisterRequest request);
    List<UserResponse> getAllUsers();
    UserResponse getUserById(UUID id);
    UserResponse getUserByEmail(String email);
    UserResponse updateUserRole(UUID id, Role role);
    void deleteUser(UUID id);
}
