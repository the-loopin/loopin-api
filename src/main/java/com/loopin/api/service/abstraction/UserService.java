package com.loopin.api.service.abstraction;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.dto.user.request.UserRegisterRequest;
import com.loopin.api.dto.user.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse registerUser(UserRegisterRequest request);
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse updateUserRole(Long id, Role role);
    void deleteUser(Long id);
}
