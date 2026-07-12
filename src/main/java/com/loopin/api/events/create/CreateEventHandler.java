package com.loopin.api.events.create;

import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.interest.EventInterestManager;
import com.loopin.api.events.shared.moderation.EventModerationManager;
import com.loopin.api.events.shared.policy.EventLifecyclePolicy;
import com.loopin.api.events.shared.validation.EventValidator;
import com.loopin.api.events.shared.validation.EventRequestValidator;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.notifications.api.NotificationWriter;
import com.loopin.api.recommendation.api.RecommendationIndexer;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateEventHandler {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventFinder eventFinder;
    private final EventValidator eventValidator;
    private final EventRequestValidator eventRequestValidator;
    private final EventInterestManager eventInterestManager;
    private final EventModerationManager eventModerationManager;
    private final RecommendationIndexer recommendationIndexer;
    private final NotificationWriter notificationWriter;

    /**
     * List cache keys include arbitrary filters, sort orders, and page values, so a precise key
     * cannot be derived safely from this command. Clear all public-list variants after a write.
     */
    @CacheEvict(value = "publishedEvents", allEntries = true)
    @Transactional
    public EventResponse handle(CreateEventCommand command) {
        EventCreateRequest request = command.request();
        eventRequestValidator.validate(request);
        User currentUser = eventFinder.findCurrentUser(command.currentUsername());
        eventValidator.validateNoDuplicate(request.getTitle(), request.getCity(), request.getStartDateTime());

        Event event = eventMapper.toEntity(request);
        event.setOwner(currentUser);
        event.setStatus(EventLifecyclePolicy.initialStatus());
        eventModerationManager.apply(event, request.getTitle(), request.getDescription());

        Event savedEvent = eventRepository.saveAndFlush(event);
        eventInterestManager.replace(savedEvent, request.getInterestIds());
        recommendationIndexer.index(savedEvent);
        notificationWriter.write(new NotificationCommand(
                currentUser,
                NotificationType.EVENT_UPDATE,
                "Event created",
                "Your event \"" + savedEvent.getTitle() + "\" was created.",
                NotificationReferenceType.EVENT,
                savedEvent.getPublicId()));

        return eventMapper.toResponse(savedEvent);
    }
}
