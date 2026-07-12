package com.loopin.api.events.update;

import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.access.EventAccessPolicy;
import com.loopin.api.events.shared.policy.EventLifecyclePolicy;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.interest.EventInterestManager;
import com.loopin.api.events.shared.moderation.EventModerationManager;
import com.loopin.api.events.shared.notification.EventMemberNotifier;
import com.loopin.api.events.shared.validation.EventValidator;
import com.loopin.api.events.shared.validation.EventRequestValidator;
import com.loopin.api.recommendation.api.RecommendationIndexer;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UpdateEventHandler {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventFinder eventFinder;
    private final EventAccessPolicy eventAccessPolicy;
    private final EventValidator eventValidator;
    private final EventRequestValidator eventRequestValidator;
    private final EventInterestManager eventInterestManager;
    private final EventModerationManager eventModerationManager;
    private final RecommendationIndexer recommendationIndexer;
    private final EventMemberNotifier eventMemberNotifier;

    /** Clears all filtered/pageable list variants; the command can change any discovery field. */
    @Caching(evict = {
        @CacheEvict(value = "publishedEvents", allEntries = true),
        @CacheEvict(value = "eventById", key = "#command.id")
    })
    @Transactional
    public EventResponse handle(UpdateEventCommand command) {
        EventUpdateRequest request = command.request();
        eventRequestValidator.validate(request);
        User currentUser = eventFinder.findCurrentUser(command.currentUsername());
        eventValidator.validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        eventValidator.validatePrice(request.getIsFree(), request.getPrice());

        Event event = eventFinder.findActiveEventById(command.id());
        EventLifecyclePolicy.requireEditable(event);
        eventAccessPolicy.requireOwnerOrAdmin(event, currentUser);
        boolean moderationRequired = !Objects.equals(event.getTitle(), request.getTitle())
                || !Objects.equals(event.getDescription(), request.getDescription());

        eventMapper.updateEntity(event, request);
        if (moderationRequired) {
            eventModerationManager.apply(event, request.getTitle(), request.getDescription());
        }
        eventInterestManager.replace(event, request.getInterestIds());

        Event savedEvent = eventRepository.save(event);
        recommendationIndexer.index(savedEvent);
        eventMemberNotifier.notifyMembers(
                savedEvent,
                "Event updated",
                "\"" + savedEvent.getTitle() + "\" has been updated.");
        return eventMapper.toResponse(savedEvent);
    }
}
