package com.loopin.api.service.implementation;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.admin.response.DashboardStatsResponse;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.dto.user.response.UserResponse;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.User;
import com.loopin.api.mapper.EventMapper;
import com.loopin.api.mapper.UserMapper;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceImplTest {

    private static final UUID TARGET_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TARGET_EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private UserRepository userRepository;
    private EventRepository eventRepository;
    private EventGroupRepository eventGroupRepository;
    private UserMapper userMapper;
    private EventMapper eventMapper;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        eventRepository = mock(EventRepository.class);
        eventGroupRepository = mock(EventGroupRepository.class);
        userMapper = mock(UserMapper.class);
        eventMapper = mock(EventMapper.class);

        adminService = new AdminServiceImpl(
                userRepository,
                eventRepository,
                eventGroupRepository,
                userMapper,
                eventMapper
        );
    }

    @Test
    void getDashboardStats_Valid_ReturnsStats() {
        when(userRepository.countByIsActiveTrue()).thenReturn(100L);
        when(eventRepository.count(any(Specification.class))).thenReturn(50L);
        when(eventGroupRepository.count()).thenReturn(25L);

        DashboardStatsResponse response = adminService.getDashboardStats();

        assertEquals(100L, response.getTotalUsers());
        assertEquals(50L, response.getActiveEvents());
        assertEquals(25L, response.getTotalGroups());
    }

    @Test
    void getUsers_Valid_ReturnsPagedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = user(1L, TARGET_USER_ID, Role.USER, "user@test.com");
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAllByIsActiveTrue(pageable)).thenReturn(userPage);
        UserResponse userResponse = new UserResponse();
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        Page<UserResponse> response = adminService.getUsers(pageable);

        assertEquals(1, response.getContent().size());
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUserRole_ValidRoleUpdate_ReturnsUpdatedUser() {
        User targetUser = user(1L, TARGET_USER_ID, Role.USER, "target@test.com");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(TARGET_USER_ID))
                .thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        UserResponse mockResponse = new UserResponse();
        when(userMapper.toResponse(any(User.class))).thenReturn(mockResponse);

        UserResponse response = adminService.updateUserRole(TARGET_USER_ID, Role.ADMIN, ADMIN_USER_ID.toString());

        assertEquals(Role.ADMIN, targetUser.getRole());
        verify(userRepository).save(targetUser);
        assertEquals(mockResponse, response);
    }

    @Test
    void updateUserRole_SelfDemote_ThrowsForbidden() {
        User targetUser = user(1L, ADMIN_USER_ID, Role.ADMIN, "admin@test.com");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(ADMIN_USER_ID))
                .thenReturn(Optional.of(targetUser));

        assertThrows(
                ResponseStatusException.class,
                () -> adminService.updateUserRole(ADMIN_USER_ID, Role.USER, ADMIN_USER_ID.toString())
        );
    }

    @Test
    void updateUserRole_LastAdmin_ThrowsBadRequest() {
        User targetUser = user(1L, TARGET_USER_ID, Role.ADMIN, "admin1@test.com");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(TARGET_USER_ID))
                .thenReturn(Optional.of(targetUser));
        when(userRepository.countByRoleAndIsActiveTrue(Role.ADMIN)).thenReturn(1L);

        assertThrows(
                ResponseStatusException.class,
                () -> adminService.updateUserRole(TARGET_USER_ID, Role.USER, ADMIN_USER_ID.toString())
        );
    }

    @Test
    void deleteUser_ValidSoftDelete_MarksUserAsDeleted() {
        User targetUser = user(1L, TARGET_USER_ID, Role.USER, "target@test.com");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(TARGET_USER_ID))
                .thenReturn(Optional.of(targetUser));

        adminService.deleteUser(TARGET_USER_ID, ADMIN_USER_ID.toString());

        assertFalse(targetUser.getIsActive());
        verify(userRepository).save(targetUser);
    }

    @Test
    void deleteUser_SelfDelete_ThrowsForbidden() {
        User targetUser = user(1L, ADMIN_USER_ID, Role.ADMIN, "admin@test.com");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(ADMIN_USER_ID))
                .thenReturn(Optional.of(targetUser));

        assertThrows(
                ResponseStatusException.class,
                () -> adminService.deleteUser(ADMIN_USER_ID, ADMIN_USER_ID.toString())
        );
    }

    @Test
    void deleteUser_LastAdmin_ThrowsBadRequest() {
        User targetUser = user(1L, TARGET_USER_ID, Role.ADMIN, "admin1@test.com");
        when(userRepository.findByPublicIdAndDeletedAtIsNull(TARGET_USER_ID))
                .thenReturn(Optional.of(targetUser));
        when(userRepository.countByRoleAndIsActiveTrue(Role.ADMIN)).thenReturn(1L);

        assertThrows(
                ResponseStatusException.class,
                () -> adminService.deleteUser(TARGET_USER_ID, ADMIN_USER_ID.toString())
        );
    }

    @Test
    void getEvents_Valid_ReturnsPagedEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        Event event = event(1L, TARGET_EVENT_ID);
        Page<Event> eventPage = new PageImpl<>(List.of(event));

        when(eventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(eventPage);
        EventResponse realResponse = eventResponse(TARGET_EVENT_ID);
        when(eventMapper.toResponse(event)).thenReturn(realResponse);

        Page<EventResponse> response = adminService.getEvents(EventStatus.PUBLISHED, pageable);

        assertEquals(1, response.getContent().size());
        assertEquals(realResponse, response.getContent().get(0));
        assertEquals(realResponse.getTitle(), response.getContent().get(0).getTitle());
    }

    @Test
    void deleteEvent_ValidEvent_SetsStatusToCancelledAndSaves() {
        Event event = event(1L, TARGET_EVENT_ID);
        EventGroup group = new EventGroup();
        group.setStatus(GroupStatus.OPEN);

        when(eventRepository.findByPublicIdAndDeletedAtIsNull(TARGET_EVENT_ID))
                .thenReturn(Optional.of(event));
        when(eventGroupRepository.findByEventIdAndStatusNot(event.getId(), GroupStatus.ARCHIVED))
                .thenReturn(List.of(group));

        adminService.deleteEvent(TARGET_EVENT_ID, ADMIN_USER_ID.toString());

        assertEquals(EventStatus.CANCELLED, event.getStatus());
        assertEquals(GroupStatus.ARCHIVED, group.getStatus());
        verify(eventGroupRepository).save(group);
        verify(eventRepository).save(event);
    }

    @Test
    void deleteEvent_EventNotFound_ThrowsResourceNotFoundException() {
        when(eventRepository.findByPublicIdAndDeletedAtIsNull(TARGET_EVENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> adminService.deleteEvent(TARGET_EVENT_ID, ADMIN_USER_ID.toString())
        );
    }

    private User user(Long id, UUID publicId, Role role, String email) {
        User user = new User(email, "Test User", null);
        user.setId(id);
        user.setPublicId(publicId);
        user.setRole(role);
        user.setIsActive(true);
        return user;
    }

    private Event event(Long id, UUID publicId) {
        Event event = new Event();
        event.setId(id);
        event.setPublicId(publicId);
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }

    private EventResponse eventResponse(UUID id) {
        return new EventResponse(
                id, "Title", "Desc", com.loopin.api.common.enums.EventType.EVENT, 
                com.loopin.api.common.enums.EventCategory.TECH, "City", "Address",
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(1), true, 
                java.math.BigDecimal.ZERO, "Organizer", "Image", EventStatus.PUBLISHED, 
                new java.util.ArrayList<>(), java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
    }
}
