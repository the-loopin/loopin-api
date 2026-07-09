package com.loopin.api.service.implementation;

import com.loopin.api.common.enums.ContentModerationStatus;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.ModerationAction;
import com.loopin.api.common.enums.ModerationContentType;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.dto.moderation.response.ModerationItemResponse;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.ModerationLog;
import com.loopin.api.entity.User;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.ModerationLogRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.service.abstraction.ModerationReviewService;
import lombok.RequiredArgsConstructor;
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
    @Transactional
    public ModerationItemResponse approveEvent(UUID eventId, String adminEmail) {
        Event event = findPendingEvent(eventId);
        User admin = findAdmin(adminEmail);

        event.setModerationStatus(ContentModerationStatus.APPROVED);
        event.setModerationRejectionReason(null);
        event.setStatus(EventStatus.PUBLISHED);
        Event updatedEvent = eventRepository.save(event);
        saveLog(updatedEvent, admin, ModerationAction.APPROVED, null);

        return toResponse(updatedEvent);
    }

    @Override
    @Transactional
    public ModerationItemResponse rejectEvent(UUID eventId, String reason, String adminEmail) {
        Event event = findPendingEvent(eventId);
        User admin = findAdmin(adminEmail);

        event.setModerationStatus(ContentModerationStatus.REJECTED);
        event.setModerationRejectionReason(reason);
        event.setStatus(EventStatus.DRAFT);
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
