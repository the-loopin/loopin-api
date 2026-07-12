package com.loopin.api.moderation.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.moderation.dto.admin.response.DashboardStatsResponse;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.users.dto.response.UserResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.users.mapper.UserMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.moderation.service.AdminService;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.notifications.service.NotificationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventGroupRepository eventGroupRepository;
    private final UserMapper userMapper;
    private final EventMapper eventMapper;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;

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
    public UserResponse updateUserRole(UUID targetUserId, Role role, String currentAdminIdentifier) {
        User targetUser = userRepository.findByPublicIdAndDeletedAtIsNull(targetUserId)
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

        log.info("Admin role update completed role={} targetUserId={}", role, targetUserId);

        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID targetUserId, String currentAdminIdentifier) {
        User targetUser = userRepository.findByPublicIdAndDeletedAtIsNull(targetUserId)
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

        log.info("Admin soft-delete completed targetUserId={}", targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getEvents(EventStatus status, Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<Event> spec =
                (root, query, cb) -> cb.isNull(root.get("deletedAt"));
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return eventRepository.findAll(spec, pageable).map(eventMapper::toResponse);
    }

    @Override
    /** Administrator cancellation can affect any cached public-list filter and its detail entry. */
    @Caching(evict = {
            @CacheEvict(value = "publishedEvents", allEntries = true),
            @CacheEvict(value = "eventById", key = "#eventId")
    })
    @Transactional
    public void deleteEvent(UUID eventId, String currentAdminIdentifier) {
        Event event = eventRepository.findByPublicIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        notificationService.createAll(groupMemberRepository.findDistinctActiveUsersByEventId(event.getId())
                .stream()
                .map(recipient -> new NotificationCommand(
                        recipient,
                        NotificationType.EVENT_UPDATE,
                        "Event cancelled",
                        "\"" + event.getTitle() + "\" has been cancelled.",
                        NotificationReferenceType.EVENT,
                        event.getPublicId()))
                .toList());
        archiveGroupsForEvent(event.getId());
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        log.info("Admin event deletion completed eventId={}", eventId);
    }

    private void archiveGroupsForEvent(Long eventId) {
        List<EventGroup> groups = eventGroupRepository.findByEventIdAndStatusNot(eventId, GroupStatus.ARCHIVED);
        groups.forEach(group -> {
            group.setStatus(GroupStatus.ARCHIVED);
            eventGroupRepository.save(group);
        });
    }

    private boolean isSameUser(User user, String adminIdentifier) {
        if (adminIdentifier == null || adminIdentifier.isBlank()) {
            return false;
        }
        if (user.getPublicId() != null && user.getPublicId().toString().equals(adminIdentifier)) {
            return true;
        }
        return user.getEmail() != null && user.getEmail().equalsIgnoreCase(adminIdentifier);
    }
}
