package com.loopin.api.events.update;

import com.loopin.api.common.cache.CacheNames;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.access.EventAccessPolicy;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.interest.EventInterestManager;
import com.loopin.api.events.shared.moderation.EventModerationManager;
import com.loopin.api.events.shared.notification.EventMemberNotifier;
import com.loopin.api.events.shared.policy.EventLifecyclePolicy;
import com.loopin.api.events.shared.validation.EventRequestValidator;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import com.loopin.api.recommendation.api.RecommendationIndexer;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.loopin.api.media.enums.MediaPurpose.EVENT_IMAGE;

@Component
@RequiredArgsConstructor
public class UpdateEventHandler {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventFinder eventFinder;
    private final EventAccessPolicy eventAccessPolicy;
    private final EventRequestValidator eventRequestValidator;
    private final EventInterestManager eventInterestManager;
    private final EventModerationManager eventModerationManager;
    private final RecommendationIndexer recommendationIndexer;
    private final EventMemberNotifier eventMemberNotifier;
    private final MediaAttachmentManager mediaAttachmentManager;

    @Caching(
        evict = {
            @CacheEvict(
                value = CacheNames.PUBLISHED_EVENTS,
                allEntries = true
            ),
            @CacheEvict(
                value = "eventById",
                key = "#command.id"
            )
        }
    )
    @Transactional
    public EventResponse handle(UpdateEventCommand command) {
        EventUpdateRequest request = command.request();

        eventRequestValidator.validate(request);

        User currentUser = eventFinder.findCurrentUser(
            command.currentUsername()
        );

        Event event =
            eventFinder.findActiveEventById(command.id());

        EventLifecyclePolicy.requireEditable(event);

        eventAccessPolicy.requireOwnerOrAdmin(
            event,
            currentUser
        );

        boolean moderationRequired =
            !Objects.equals(
                event.getTitle(),
                request.getTitle()
            )
                || !Objects.equals(
                event.getDescription(),
                request.getDescription()
            );

        MediaAsset updatedImage =
            mediaAttachmentManager.replace(
                event.getImageMedia(),
                request.getImageMediaId(),
                currentUser,
                EVENT_IMAGE
            );

        /*
         * Mapper imageMedia field-ini ignore edir.
         */
        eventMapper.updateEntity(event, request);
        event.setImageMedia(updatedImage);

        if (moderationRequired) {
            eventModerationManager.apply(
                event,
                request.getTitle(),
                request.getDescription()
            );
        }

        eventInterestManager.replace(
            event,
            request.getInterestIds()
        );

        Event savedEvent =
            eventRepository.save(event);

        recommendationIndexer.index(savedEvent);

        eventMemberNotifier.notifyMembers(
            savedEvent,
            "Event updated",
            "\""
                + savedEvent.getTitle()
                + "\" has been updated."
        );

        return eventMapper.toResponse(savedEvent);
    }
}
