package com.loopin.api.events.create;

import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.interest.EventInterestManager;
import com.loopin.api.events.shared.moderation.EventModerationManager;
import com.loopin.api.events.shared.validation.EventRequestValidationException;
import com.loopin.api.events.shared.validation.EventRequestValidator;
import com.loopin.api.events.shared.validation.EventValidator;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import com.loopin.api.notifications.api.NotificationWriter;
import com.loopin.api.recommendation.api.RecommendationIndexer;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.loopin.api.media.enums.MediaPurpose.EVENT_IMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateEventHandlerTest {

    private EventRepository eventRepository;
    private EventMapper eventMapper;
    private EventFinder eventFinder;
    private EventValidator eventValidator;
    private EventRequestValidator eventRequestValidator;
    private EventInterestManager eventInterestManager;
    private EventModerationManager eventModerationManager;
    private RecommendationIndexer recommendationIndexer;
    private NotificationWriter notificationWriter;
    private MediaAttachmentManager mediaAttachmentManager;

    private CreateEventHandler handler;

    @BeforeEach
    void setUp() {
        eventRepository =
            mock(EventRepository.class);

        eventMapper =
            mock(EventMapper.class);

        eventFinder =
            mock(EventFinder.class);

        eventValidator =
            mock(EventValidator.class);

        eventRequestValidator =
            mock(EventRequestValidator.class);

        eventInterestManager =
            mock(EventInterestManager.class);

        eventModerationManager =
            mock(EventModerationManager.class);

        recommendationIndexer =
            mock(RecommendationIndexer.class);

        notificationWriter =
            mock(NotificationWriter.class);

        mediaAttachmentManager =
            mock(MediaAttachmentManager.class);

        handler = new CreateEventHandler(
            eventRepository,
            eventMapper,
            eventFinder,
            eventValidator,
            eventRequestValidator,
            eventInterestManager,
            eventModerationManager,
            recommendationIndexer,
            notificationWriter,
            mediaAttachmentManager
        );
    }

    @Test
    void handle_validCommand_attachesImageAndPersistsEvent() {
        EventCreateRequest request =
            validRequest();

        UUID imageMediaId =
            UUID.randomUUID();

        request.setImageMediaId(
            imageMediaId
        );

        User owner =
            new User(
                "owner@example.test",
                "Owner",
                null
            );

        Event event = new Event();

        event.setTitle(request.getTitle());
        event.setPublicId(UUID.randomUUID());

        MediaAsset imageMedia =
            mock(MediaAsset.class);

        EventResponse response =
            mock(EventResponse.class);

        when(
            eventFinder.findCurrentUser(
                "owner@example.test"
            )
        ).thenReturn(owner);

        when(
            mediaAttachmentManager.attach(
                imageMediaId,
                owner,
                EVENT_IMAGE
            )
        ).thenReturn(imageMedia);

        when(
            eventMapper.toEntity(request)
        ).thenReturn(event);

        when(
            eventRepository.saveAndFlush(event)
        ).thenReturn(event);

        when(
            eventMapper.toResponse(event)
        ).thenReturn(response);

        EventResponse result =
            handler.handle(
                new CreateEventCommand(
                    request,
                    "owner@example.test"
                )
            );

        assertEquals(response, result);

        assertSame(
            owner,
            event.getOwner()
        );

        assertSame(
            imageMedia,
            event.getImageMedia()
        );

        assertEquals(
            EventStatus.PUBLISHED,
            event.getStatus()
        );

        verify(eventRequestValidator)
            .validate(request);

        verify(eventValidator)
            .validateNoDuplicate(
                request.getTitle(),
                request.getCity(),
                request.getStartDateTime()
            );

        verify(mediaAttachmentManager)
            .attach(
                imageMediaId,
                owner,
                EVENT_IMAGE
            );

        verify(eventModerationManager)
            .apply(
                event,
                request.getTitle(),
                request.getDescription()
            );

        verify(eventInterestManager)
            .replace(
                event,
                request.getInterestIds()
            );

        verify(recommendationIndexer)
            .index(event);

        verify(notificationWriter)
            .write(any());
    }

    @Test
    void handle_withoutImage_createsEventWithoutImage() {
        EventCreateRequest request =
            validRequest();

        request.setImageMediaId(null);

        User owner =
            new User(
                "owner@example.test",
                "Owner",
                null
            );

        Event event = new Event();

        when(
            eventFinder.findCurrentUser(
                "owner@example.test"
            )
        ).thenReturn(owner);

        when(
            mediaAttachmentManager.attach(
                null,
                owner,
                EVENT_IMAGE
            )
        ).thenReturn(null);

        when(
            eventMapper.toEntity(request)
        ).thenReturn(event);

        when(
            eventRepository.saveAndFlush(event)
        ).thenReturn(event);

        handler.handle(
            new CreateEventCommand(
                request,
                "owner@example.test"
            )
        );

        verify(mediaAttachmentManager)
            .attach(
                null,
                owner,
                EVENT_IMAGE
            );
    }

    @Test
    void handle_duplicateEvent_stopsBeforeMediaAttachment() {
        EventCreateRequest request =
            validRequest();

        User owner = new User();

        when(
            eventFinder.findCurrentUser(
                "owner@example.test"
            )
        ).thenReturn(owner);

        doThrow(
            new DuplicateResourceException(
                "duplicate"
            )
        ).when(eventValidator)
            .validateNoDuplicate(
                request.getTitle(),
                request.getCity(),
                request.getStartDateTime()
            );

        assertThrows(
            DuplicateResourceException.class,
            () -> handler.handle(
                new CreateEventCommand(
                    request,
                    "owner@example.test"
                )
            )
        );

        verify(
            mediaAttachmentManager,
            never()
        ).attach(
            any(),
            any(),
            any()
        );

        verify(
            eventRepository,
            never()
        ).saveAndFlush(any());

        verify(
            recommendationIndexer,
            never()
        ).index(any());

        verify(
            notificationWriter,
            never()
        ).write(any());
    }

    @Test
    void handle_invalidRequest_stopsBeforeMediaAttachment() {
        EventCreateRequest request =
            validRequest();

        doThrow(
            new EventRequestValidationException(
                Map.of(
                    "endDateTime",
                    "invalid"
                )
            )
        ).when(eventRequestValidator)
            .validate(request);

        assertThrows(
            EventRequestValidationException.class,
            () -> handler.handle(
                new CreateEventCommand(
                    request,
                    "owner@example.test"
                )
            )
        );

        verify(
            mediaAttachmentManager,
            never()
        ).attach(
            any(),
            any(),
            any()
        );

        verify(
            eventRepository,
            never()
        ).saveAndFlush(any());
    }

    private EventCreateRequest validRequest() {
        EventCreateRequest request =
            new EventCreateRequest();

        request.setTitle("Event title");
        request.setDescription(
            "Event description"
        );
        request.setType(EventType.EVENT);
        request.setCategory(EventCategory.TECH);
        request.setCity("Baku");

        request.setStartDateTime(
            LocalDateTime.of(
                2030,
                1,
                1,
                10,
                0
            )
        );

        request.setEndDateTime(
            LocalDateTime.of(
                2030,
                1,
                1,
                12,
                0
            )
        );

        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);
        request.setOrganizerName("Loopin");

        return request;
    }
}
