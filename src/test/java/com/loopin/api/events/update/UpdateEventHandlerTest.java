package com.loopin.api.events.update;

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
import com.loopin.api.events.shared.validation.EventValidator;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateEventHandlerTest {

    @Test
    void handle_updatesDetailsAndRefreshesDependentReadModels() {
        EventRepository repository = mock(EventRepository.class);
        EventMapper mapper = mock(EventMapper.class);
        EventFinder finder = mock(EventFinder.class);
        EventAccessPolicy accessPolicy = mock(EventAccessPolicy.class);
        EventValidator validator = mock(EventValidator.class);
        EventInterestManager interestManager = mock(EventInterestManager.class);
        EventModerationManager moderationManager = mock(EventModerationManager.class);
        EventEmbeddingService embeddingService = mock(EventEmbeddingService.class);
        EventMemberNotifier memberNotifier = mock(EventMemberNotifier.class);
        UpdateEventHandler handler = new UpdateEventHandler(repository, mapper, finder, accessPolicy, validator,
                interestManager, moderationManager, embeddingService, memberNotifier);
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
        verify(validator).validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        verify(validator).validatePrice(true, BigDecimal.ZERO);
        verify(mapper).updateEntity(event, request);
        verify(moderationManager).apply(event, "New title", "New description");
        verify(interestManager).replace(event, request.getInterestIds());
        verify(embeddingService).indexEvent(event);
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
}
