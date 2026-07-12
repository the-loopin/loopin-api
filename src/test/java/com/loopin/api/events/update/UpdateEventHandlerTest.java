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
import com.loopin.api.recommendation.api.RecommendationIndexer;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateEventHandlerTest {

    @Test
    void handle_updatesDetailsAndRefreshesDependentReadModels() {
        EventRepository repository = mock(EventRepository.class);
        EventMapper mapper = mock(EventMapper.class);
        EventFinder finder = mock(EventFinder.class);
        EventAccessPolicy accessPolicy = mock(EventAccessPolicy.class);
        EventRequestValidator requestValidator = mock(EventRequestValidator.class);
        EventInterestManager interestManager = mock(EventInterestManager.class);
        EventModerationManager moderationManager = mock(EventModerationManager.class);
        RecommendationIndexer recommendationIndexer = mock(RecommendationIndexer.class);
        EventMemberNotifier memberNotifier = mock(EventMemberNotifier.class);
        UpdateEventHandler handler = new UpdateEventHandler(repository, mapper, finder, accessPolicy, requestValidator,
                interestManager, moderationManager, recommendationIndexer, memberNotifier);
        UUID id = UUID.randomUUID();
        Event event = event(id, "Old title", "Old description");
        EventUpdateRequest request = request("New title", "New description");
        User user = new User("owner@loopin.test", "Owner", null);
        EventResponse response = mock(EventResponse.class);
        when(finder.findCurrentUser("owner@loopin.test")).thenReturn(user);
        when(finder.findActiveEventById(id)).thenReturn(event);
        when(repository.save(event)).thenReturn(event);
        when(mapper.toResponse(event)).thenReturn(response);

        EventResponse result = handler.handle(new UpdateEventCommand(id, request, "owner@loopin.test"));

        assertEquals(response, result);
        verify(accessPolicy).requireOwnerOrAdmin(event, user);
        verify(requestValidator).validate(request);
        verify(mapper).updateEntity(event, request);
        verify(moderationManager).apply(event, "New title", "New description");
        verify(interestManager).replace(event, request.getInterestIds());
        verify(recommendationIndexer).index(event);
        verify(memberNotifier).notifyMembers(event, "Event updated", "\"Old title\" has been updated.");
    }

    private Event event(UUID id, String title, String description) {
        Event event = new Event();
        event.setPublicId(id);
        event.setTitle(title);
        event.setDescription(description);
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }

    private EventUpdateRequest request(String title, String description) {
        EventUpdateRequest request = new EventUpdateRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setStartDateTime(LocalDateTime.now().plusDays(1));
        request.setEndDateTime(LocalDateTime.now().plusDays(2));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);
        return request;
    }
    @Test
    void handle_rejectsCancelledEventUpdate() {
        EventFinder finder = mock(EventFinder.class);
        EventRepository repository = mock(EventRepository.class);
        UpdateEventHandler handler = new UpdateEventHandler(repository, mock(EventMapper.class), finder,
                mock(EventAccessPolicy.class), mock(EventRequestValidator.class), mock(EventInterestManager.class),
                mock(EventModerationManager.class), mock(RecommendationIndexer.class), mock(EventMemberNotifier.class));

        UUID id = UUID.randomUUID();
        Event event = event(id, "Old title", "Old description");
        event.setStatus(EventStatus.CANCELLED);
        when(finder.findActiveEventById(id)).thenReturn(event);

        assertThrows(InvalidEventStateException.class, () ->
                handler.handle(new UpdateEventCommand(id, request("t", "d"), "user")));
        verify(repository, never()).save(event);
    }

    @Test
    void handle_rejectsCompletedEventUpdate() {
        EventFinder finder = mock(EventFinder.class);
        EventRepository repository = mock(EventRepository.class);
        UpdateEventHandler handler = new UpdateEventHandler(repository, mock(EventMapper.class), finder,
                mock(EventAccessPolicy.class), mock(EventRequestValidator.class), mock(EventInterestManager.class),
                mock(EventModerationManager.class), mock(RecommendationIndexer.class), mock(EventMemberNotifier.class));

        UUID id = UUID.randomUUID();
        Event event = event(id, "Old title", "Old description");
        event.setStatus(EventStatus.COMPLETED);
        when(finder.findActiveEventById(id)).thenReturn(event);

        assertThrows(InvalidEventStateException.class, () ->
                handler.handle(new UpdateEventCommand(id, request("t", "d"), "user")));
        verify(repository, never()).save(event);
    }
}
