package com.loopin.api.events.service;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.UserLoopedEvent;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLoopedEventServiceImpl implements UserLoopedEventService {

    private final UserLoopedEventRepository userLoopedEventRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public EventResponse loopIn(UUID eventId, String currentUsername) {
        User user = findUser(currentUsername);
        Event event = findEvent(eventId);

        if (!userLoopedEventRepository.existsByUserIdAndEventId(user.getId(), event.getId())) {
            userLoopedEventRepository.save(new UserLoopedEvent(user, event));
            notifyEventOwner(user, event);
        }

        return toResponseWithLoopedCount(event);
    }

    @Override
    @Transactional
    public void removeLoopIn(UUID eventId, String currentUsername) {
        User user = findUser(currentUsername);
        Event event = findEvent(eventId);

        userLoopedEventRepository.findByUserIdAndEventId(user.getId(), event.getId())
                .ifPresent(userLoopedEventRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getMyLoopedEvents(String currentUsername) {
        User user = findUser(currentUsername);
        return userLoopedEventRepository.findLoopedEventsByUserId(user.getId())
                .stream()
                .map(this::toResponseWithLoopedCount)
                .toList();
    }

    private void notifyEventOwner(User user, Event event) {
        User owner = event.getOwner();
        if (owner == null || owner.getId().equals(user.getId())) {
            return;
        }

        notificationService.create(new NotificationCommand(
                owner,
                NotificationType.EVENT_INTEREST,
                "New Loopin",
                user.getName() + " looped into \"" + event.getTitle() + "\".",
                NotificationReferenceType.EVENT,
                event.getPublicId(),
                "event-loopin:" + event.getPublicId() + ":" + user.getPublicId()));
    }

    private EventResponse toResponseWithLoopedCount(Event event) {
        EventResponse response = eventMapper.toResponse(event);
        response.setLoopedCount(userLoopedEventRepository.countByEventId(event.getId()));
        return response;
    }

    private User findUser(String currentUsername) {
        return userRepository.findByEmailAndDeletedAtIsNull(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));
    }

    private Event findEvent(UUID eventId) {
        return eventRepository.findByPublicIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }
}
