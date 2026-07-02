package com.loopin.api.service.implementation;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.admin.response.DashboardStatsResponse;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.dto.user.response.UserResponse;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.User;
import com.loopin.api.mapper.EventMapper;
import com.loopin.api.mapper.UserMapper;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventGroupRepository eventGroupRepository;
    private final UserMapper userMapper;
    private final EventMapper eventMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.countByIsActiveTrue();
        long activeEvents = eventRepository.count((root, query, cb) -> cb.and(
                cb.isNull(root.get("deletedAt")),
                cb.equal(root.get("status"), EventStatus.PUBLISHED)
        ));
        long totalGroups = eventGroupRepository.count();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeEvents(activeEvents)
                .totalGroups(totalGroups)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAllByIsActiveTrue(pageable);
        return userPage.map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long targetUserId, Role role, String currentAdminIdentifier) {
        User targetUser = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        if (isSameUser(targetUser, currentAdminIdentifier)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot change the role of their own account");
        }

        if (targetUser.getRole() == Role.ADMIN && role != Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);
            if (activeAdminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot demote the last remaining admin in the system");
            }
        }

        targetUser.setRole(role);
        User updatedUser = userRepository.save(targetUser);

        log.info("Admin {} performed role update to {} on user {}", currentAdminIdentifier, role, targetUserId);

        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long targetUserId, String currentAdminIdentifier) {
        User targetUser = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        if (isSameUser(targetUser, currentAdminIdentifier)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot delete their own account");
        }

        if (targetUser.getRole() == Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);
            if (activeAdminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the last remaining admin in the system");
            }
        }

        targetUser.setIsActive(false);
        targetUser.markAsDeleted();
        userRepository.save(targetUser);

        log.info("Admin {} performed soft-delete on user {}", currentAdminIdentifier, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getEvents(EventStatus status, Pageable pageable) {
        Page<Event> eventPage = (status != null)
                ? eventRepository.findByStatus(status, pageable)
                : eventRepository.findAll(pageable);
        return eventPage.map(eventMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId, String currentAdminIdentifier) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        log.info("Admin {} performed deletion on event {}", currentAdminIdentifier, eventId);
    }

    private boolean isSameUser(User user, String adminIdentifier) {
        if (adminIdentifier == null || adminIdentifier.isBlank()) {
            return false;
        }
        if (user.getId() != null && user.getId().toString().equals(adminIdentifier)) {
            return true;
        }
        return user.getEmail() != null && user.getEmail().equalsIgnoreCase(adminIdentifier);
    }
}
