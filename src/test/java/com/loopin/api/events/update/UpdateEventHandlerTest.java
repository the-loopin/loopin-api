package com.loopin.api.events.update;

import com.loopin.api.common.exception.InvalidEventStateException;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.access.EventAccessPolicy;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.interest.EventInterestManager;
import com.loopin.api.events.shared.moderation.EventModerationManager;
import com.loopin.api.events.shared.notification.EventMemberNotifier;
import com.loopin.api.events.shared.validation.EventRequestValidator;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import com.loopin.api.recommendation.api.RecommendationIndexer;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.loopin.api.media.enums.MediaPurpose.EVENT_IMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateEventHandlerTest {

    private EventRepository repository;
    private EventMapper mapper;
    private EventFinder finder;
    private EventAccessPolicy accessPolicy;
    private EventRequestValidator requestValidator;
    private EventInterestManager interestManager;
    private EventModerationManager moderationManager;
    private RecommendationIndexer recommendationIndexer;
    private EventMemberNotifier memberNotifier;
    private MediaAttachmentManager mediaAttachmentManager;

    private UpdateEventHandler handler;

    @BeforeEach
    void setUp() {
        repository =
            mock(EventRepository.class);

        mapper =
            mock(EventMapper.class);

        finder =
            mock(EventFinder.class);

        accessPolicy =
            mock(EventAccessPolicy.class);

        requestValidator =
            mock(EventRequestValidator.class);

        interestManager =
            mock(EventInterestManager.class);

        moderationManager =
            mock(EventModerationManager.class);

        recommendationIndexer =
            mock(RecommendationIndexer.class);

        memberNotifier =
            mock(EventMemberNotifier.class);

        mediaAttachmentManager =
            mock(MediaAttachmentManager.class);

        handler = new UpdateEventHandler(
            repository,
            mapper,
            finder,
            accessPolicy,
            requestValidator,
            interestManager,
            moderationManager,
            recommendationIndexer,
            memberNotifier,
            mediaAttachmentManager
        );
    }

    @Test
    void handle_replacesImageAndUpdatesEvent() {
        UUID eventId = UUID.randomUUID();
        UUID newImageId = UUID.randomUUID();

        Event event = event(
            eventId,
            "Old title",
            "Old description"
        );

        MediaAsset oldImage =
            mock(MediaAsset.class);

        MediaAsset newImage =
            mock(MediaAsset.class);

        event.setImageMedia(oldImage);

        EventUpdateRequest request =
            request(
                "New title",
                "New description"
            );

        request.setImageMediaId(
            newImageId
        );

        User user =
            new User(
                "owner@loopin.test",
                "Owner",
                null
            );

        EventResponse response =
            mock(EventResponse.class);

        when(
            finder.findCurrentUser(
                "owner@loopin.test"
            )
        ).thenReturn(user);

        when(
            finder.findActiveEventById(
                eventId
            )
        ).thenReturn(event);

        when(
            mediaAttachmentManager.replace(
                oldImage,
                newImageId,
                user,
                EVENT_IMAGE
            )
        ).thenReturn(newImage);

        when(
            repository.save(event)
        ).thenReturn(event);

        when(
            mapper.toResponse(event)
        ).thenReturn(response);

        EventResponse result =
            handler.handle(
                new UpdateEventCommand(
                    eventId,
                    request,
                    "owner@loopin.test"
                )
            );

        assertEquals(response, result);

        assertSame(
            newImage,
            event.getImageMedia()
        );

        verify(accessPolicy)
            .requireOwnerOrAdmin(
                event,
                user
            );

        verify(requestValidator)
            .validate(request);

        verify(mediaAttachmentManager)
            .replace(
                oldImage,
                newImageId,
                user,
                EVENT_IMAGE
            );

        verify(mapper)
            .updateEntity(
                event,
                request
            );

        verify(moderationManager)
            .apply(
                event,
                "New title",
                "New description"
            );

        verify(interestManager)
            .replace(
                event,
                request.getInterestIds()
            );

        verify(recommendationIndexer)
            .index(event);
    }

    @Test
    void handle_nullImageMediaId_removesCurrentImage() {
        UUID eventId = UUID.randomUUID();

        Event event = event(
            eventId,
            "Old title",
            "Old description"
        );

        MediaAsset oldImage =
            mock(MediaAsset.class);

        event.setImageMedia(oldImage);

        EventUpdateRequest request =
            request(
                "Old title",
                "Old description"
            );

        request.setImageMediaId(null);

        User user =
            new User(
                "owner@loopin.test",
                "Owner",
                null
            );

        when(
            finder.findCurrentUser(
                "owner@loopin.test"
            )
        ).thenReturn(user);

        when(
            finder.findActiveEventById(
                eventId
            )
        ).thenReturn(event);

        when(
            mediaAttachmentManager.replace(
                oldImage,
                null,
                user,
                EVENT_IMAGE
            )
        ).thenReturn(null);

        when(
            repository.save(event)
        ).thenReturn(event);

        handler.handle(
            new UpdateEventCommand(
                eventId,
                request,
                "owner@loopin.test"
            )
        );

        assertSame(
            null,
            event.getImageMedia()
        );

        verify(mediaAttachmentManager)
            .replace(
                oldImage,
                null,
                user,
                EVENT_IMAGE
            );

        verify(
            moderationManager,
            never()
        ).apply(
            any(),
            any(),
            any()
        );
    }

    @Test
    void handle_rejectsCancelledEventBeforeMediaChange() {
        UUID eventId = UUID.randomUUID();

        Event event = event(
            eventId,
            "Old title",
            "Old description"
        );

        event.setStatus(EventStatus.CANCELLED);

        when(
            finder.findActiveEventById(
                eventId
            )
        ).thenReturn(event);

        assertThrows(
            InvalidEventStateException.class,
            () -> handler.handle(
                new UpdateEventCommand(
                    eventId,
                    request("t", "d"),
                    "user"
                )
            )
        );

        verify(
            mediaAttachmentManager,
            never()
        ).replace(
            any(),
            any(),
            any(),
            any()
        );

        verify(
            repository,
            never()
        ).save(event);
    }

    @Test
    void handle_rejectsCompletedEventBeforeMediaChange() {
        UUID eventId = UUID.randomUUID();

        Event event = event(
            eventId,
            "Old title",
            "Old description"
        );

        event.setStatus(EventStatus.COMPLETED);

        when(
            finder.findActiveEventById(
                eventId
            )
        ).thenReturn(event);

        assertThrows(
            InvalidEventStateException.class,
            () -> handler.handle(
                new UpdateEventCommand(
                    eventId,
                    request("t", "d"),
                    "user"
                )
            )
        );

        verify(
            mediaAttachmentManager,
            never()
        ).replace(
            any(),
            any(),
            any(),
            any()
        );

        verify(
            repository,
            never()
        ).save(event);
    }

    private Event event(
        UUID id,
        String title,
        String description
    ) {
        Event event = new Event();

        event.setPublicId(id);
        event.setTitle(title);
        event.setDescription(description);
        event.setStatus(EventStatus.PUBLISHED);

        return event;
    }

    private EventUpdateRequest request(
        String title,
        String description
    ) {
        EventUpdateRequest request =
            new EventUpdateRequest();

        request.setTitle(title);
        request.setDescription(description);

        request.setStartDateTime(
            LocalDateTime.now()
                .plusDays(1)
        );

        request.setEndDateTime(
            LocalDateTime.now()
                .plusDays(2)
        );

        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);

        return request;
    }
}
