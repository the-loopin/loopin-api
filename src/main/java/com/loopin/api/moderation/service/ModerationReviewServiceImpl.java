package com.loopin.api.moderation.service;

import com.loopin.api.common.cache.CacheNames;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import com.loopin.api.moderation.enums.ModerationAction;
import com.loopin.api.moderation.enums.ModerationContentType;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.moderation.dto.response.ModerationItemResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.moderation.entity.ModerationLog;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.policy.EventLifecyclePolicy;
import com.loopin.api.moderation.repository.ModerationLogRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.moderation.service.ModerationReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModerationReviewServiceImpl implements ModerationReviewService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ModerationLogRepository moderationLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ModerationItemResponse> getPendingItems(Pageable pageable) {
        return eventRepository.findByModerationStatusAndDeletedAtIsNull(
                        ContentModerationStatus.PENDING_REVIEW,
                        pageable)
                .map(this::toResponse);
    }

    @Override
    /** Approval can make an event visible in any cached filter/page combination. */
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PUBLISHED_EVENTS, allEntries = true),
            @CacheEvict(value = "eventById", key = "#eventId")
    })
    @Transactional
    public ModerationItemResponse approveEvent(UUID eventId, String adminEmail) {
        Event event = findPendingEvent(eventId);
        User admin = findAdmin(adminEmail);

        event.setModerationStatus(ContentModerationStatus.APPROVED);
        event.setModerationRejectionReason(null);
        EventLifecyclePolicy.approveModeration(event);
        Event updatedEvent = eventRepository.save(event);
        saveLog(updatedEvent, admin, ModerationAction.APPROVED, null);

        return toResponse(updatedEvent);
    }

    @Override
    /** Rejection removes public visibility and must also invalidate an existing detail response. */
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PUBLISHED_EVENTS, allEntries = true),
            @CacheEvict(value = "eventById", key = "#eventId")
    })
    @Transactional
    public ModerationItemResponse rejectEvent(UUID eventId, String reason, String adminEmail) {
        Event event = findPendingEvent(eventId);
        User admin = findAdmin(adminEmail);

        event.setModerationStatus(ContentModerationStatus.REJECTED);
        event.setModerationRejectionReason(reason);
        EventLifecyclePolicy.rejectModeration(event);
        Event updatedEvent = eventRepository.save(event);
        saveLog(updatedEvent, admin, ModerationAction.REJECTED, reason);

        return toResponse(updatedEvent);
    }

    private Event findPendingEvent(UUID eventId) {
        Event event = eventRepository.findByPublicIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (event.getModerationStatus() != ContentModerationStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event is not pending moderation review");
        }
        return event;
    }

    private User findAdmin(String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userRepository.findByEmailAndDeletedAtIsNull(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminEmail));
    }

    private void saveLog(Event event, User admin, ModerationAction action, String reason) {
        ModerationLog moderationLog = new ModerationLog();
        moderationLog.setContentType(ModerationContentType.EVENT);
        moderationLog.setContentPublicId(event.getPublicId());
        moderationLog.setAction(action);
        moderationLog.setRejectionReason(reason);
        moderationLog.setAdmin(admin);
        moderationLogRepository.save(moderationLog);
    }

    private ModerationItemResponse toResponse(Event event) {
        return ModerationItemResponse.builder()
                .id(event.getPublicId())
                .contentType(ModerationContentType.EVENT)
                .title(event.getTitle())
                .description(event.getDescription())
                .moderationStatus(event.getModerationStatus())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
