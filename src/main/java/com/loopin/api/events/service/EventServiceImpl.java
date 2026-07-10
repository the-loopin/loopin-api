package com.loopin.api.events.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.interest.EventInterestManager;
import com.loopin.api.events.shared.moderation.EventModerationManager;
import com.loopin.api.events.shared.validation.EventValidator;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import com.loopin.api.events.service.EventService;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.notifications.service.NotificationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventGroupRepository eventGroupRepository;
    private final EventInterestRepository eventInterestRepository;
    private final EventEmbeddingService eventEmbeddingService;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;
    private final EventFinder eventFinder;
    private final EventValidator eventValidator;
    private final EventInterestManager eventInterestManager;
    private final EventModerationManager eventModerationManager;

    @Override
    @Caching(evict = {
        @CacheEvict(value = "publishedEvents", allEntries = true),
        @CacheEvict(value = "eventById", key = "#id")
    })
    @Transactional
    public EventResponse updateEvent(UUID id, EventUpdateRequest request, String currentUsername) {
        User currentUser = eventFinder.findCurrentUser(currentUsername);
        eventValidator.validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        eventValidator.validatePrice(request.getIsFree(), request.getPrice());

        Event event = eventFinder.findActiveEventById(id);
        validateOwnerOrAdmin(event, currentUser);
        EventStatus previousStatus = event.getStatus();
        eventMapper.updateEntity(event, request);
        eventModerationManager.apply(event, request.getTitle(), request.getDescription());
        eventInterestManager.replace(event, request.getInterestIds());

        Event savedEvent = eventRepository.save(event);
        eventEmbeddingService.indexEvent(savedEvent);
        boolean cancelled = savedEvent.getStatus() == EventStatus.CANCELLED
                && previousStatus != EventStatus.CANCELLED;
        notifyEventMembers(
                savedEvent,
                cancelled ? "Event cancelled" : "Event updated",
                cancelled
                        ? "\"" + savedEvent.getTitle() + "\" has been cancelled."
                        : "\"" + savedEvent.getTitle() + "\" has been updated.");
        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "publishedEvents", allEntries = true),
        @CacheEvict(value = "eventById", key = "#id")
    })
    @Transactional
    public void deleteEvent(UUID id, String currentUsername) {
        User currentUser = eventFinder.findCurrentUser(currentUsername);
        Event event = eventFinder.findActiveEventById(id);
        validateOwnerOrAdmin(event, currentUser);
        notifyEventMembers(
                event,
                "Event cancelled",
                "\"" + event.getTitle() + "\" has been cancelled.");
        eventInterestRepository.deleteByEvent_Id(event.getId());
        archiveGroupsForEvent(event.getId());
        event.markAsDeleted();
        eventRepository.save(event);
    }

    private void notifyEventMembers(Event event, String title, String message) {
        List<NotificationCommand> commands = groupMemberRepository
                .findDistinctActiveUsersByEventId(event.getId())
                .stream()
                .map(recipient -> new NotificationCommand(
                        recipient,
                        NotificationType.EVENT_UPDATE,
                        title,
                        message,
                        NotificationReferenceType.EVENT,
                        event.getPublicId()))
                .toList();
        notificationService.createAll(commands);
    }

    private void archiveGroupsForEvent(Long eventId) {
        List<EventGroup> groups = eventGroupRepository.findByEventIdAndStatusNot(eventId, GroupStatus.ARCHIVED);
        groups.forEach(group -> {
            group.setStatus(GroupStatus.ARCHIVED);
            eventGroupRepository.save(group);
        });
    }

    private void validateOwnerOrAdmin(Event event, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (event.getOwner() != null && event.getOwner().getId().equals(currentUser.getId())) {
            return;
        }

        throw new ForbiddenAccessException("Only the event owner or an admin can modify this event");
    }

}


