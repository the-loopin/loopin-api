package com.loopin.api.core.users.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.core.users.dto.request.UserRegisterRequest;
import com.loopin.api.core.users.dto.response.UserResponse;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.core.users.mapper.UserMapper;
import com.loopin.api.core.users.repository.UserRepository;
import com.loopin.api.core.interests.repository.UserInterestRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "test@example.com";

    private UserRepository userRepository;
    private UserInterestRepository userInterestRepository;

    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userInterestRepository = mock(UserInterestRepository.class);

        userMapper = mock(UserMapper.class);
        userService = new UserServiceImpl(userRepository, userInterestRepository, userMapper);
    }

    @Test
    void registerUser_ValidRequest_CreatesUser() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail(EMAIL);
        request.setName("Test Name");

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

        User savedUser = user(1L, USER_ID, Role.USER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse mockResponse = new UserResponse();
        when(userMapper.toResponse(savedUser)).thenReturn(mockResponse);

        UserResponse result = userService.registerUser(request);

        verify(userRepository).save(any(User.class));
        assertEquals(mockResponse, result);
    }

    @Test
    void registerUser_EmailExists_ThrowsDuplicateResourceException() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail(EMAIL);

        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(request));
    }

    @Test
    void getAllUsers_Valid_ReturnsUsers() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(user));

        UserResponse mockResponse = new UserResponse();
        when(userMapper.toResponse(user)).thenReturn(mockResponse);

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals(mockResponse, result.get(0));
    }

    @Test
    void getUserById_Found_ReturnsUser() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.of(user));

        UserResponse mockResponse = new UserResponse();
        when(userMapper.toResponse(user)).thenReturn(mockResponse);

        UserResponse result = userService.getUserById(USER_ID);

        assertEquals(mockResponse, result);
    }

    @Test
    void getUserById_NotFound_ThrowsNoSuchElementException() {
        when(userRepository.findByPublicIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.getUserById(USER_ID));
    }

    @Test
    void getUserByEmail_Found_ReturnsUser() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.of(user));

        UserResponse mockResponse = new UserResponse();
        when(userMapper.toResponse(user)).thenReturn(mockResponse);

        UserResponse result = userService.getUserByEmail(EMAIL);

        assertEquals(mockResponse, result);
    }

    @Test
    void getUserByEmail_NotFound_ThrowsNoSuchElementException() {
        when(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.getUserByEmail(EMAIL));
    }

    @Test
    void updateUserRole_ValidUser_UpdatesAndReturnsUser() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse mockResponse = new UserResponse();
        when(userMapper.toResponse(user)).thenReturn(mockResponse);

        UserResponse result = userService.updateUserRole(USER_ID, Role.ADMIN);

        assertEquals(Role.ADMIN, user.getRole());
        verify(userRepository).save(user);
        assertEquals(mockResponse, result);
    }

    @Test
    void deleteUser_ValidUser_MarksAsDeleted() {
        User user = user(1L, USER_ID, Role.USER);
        when(userRepository.findByPublicIdAndDeletedAtIsNull(USER_ID)).thenReturn(Optional.of(user));

        userService.deleteUser(USER_ID);

        verify(userRepository).save(user);
    }

    private User user(Long id, UUID publicId, Role role) {
        User user = new User(EMAIL, "Test User", null);
        user.setId(id);
        user.setPublicId(publicId);
        user.setRole(role);
        return user;
    }
}
