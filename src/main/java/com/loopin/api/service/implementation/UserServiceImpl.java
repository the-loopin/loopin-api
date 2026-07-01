package com.loopin.api.service.implementation;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.dto.user.request.UserRegisterRequest;
import com.loopin.api.dto.user.response.UserResponse;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserProfile;
import com.loopin.api.mapper.UserMapper;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.loopin.api.exception.DuplicateResourceException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse registerUser(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered.");
        }

        if (request.getGoogleId() != null && !request.getGoogleId().isBlank()) {
            if (userRepository.existsByGoogleId(request.getGoogleId())) {
                throw new DuplicateResourceException("Google ID is already registered.");
            }
        }

        User user = new User(request.getEmail(), request.getName(), request.getGoogleId());

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setName(request.getName());
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NoSuchElementException("User not found."));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long id, Role role) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NoSuchElementException("User not found."));
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NoSuchElementException("User not found."));
        user.markAsDeleted();
        userRepository.save(user);
    }
}
