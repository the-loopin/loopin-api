package com.loopin.api.events.getrecommendedevents;

import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.specification.PublishedEventSpecifications;
import com.loopin.api.recommendation.event.EventCandidate;
import com.loopin.api.recommendation.event.EventEmbeddingRepository;
import com.loopin.api.recommendation.user.UserEmbeddingRepository;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetRecommendedEventsHandlerTest {

    private final EventRepository repository = mock(EventRepository.class);
    private final EventMapper mapper = mock(EventMapper.class);
    private final EventFinder eventFinder = mock(EventFinder.class);
    private final UserEmbeddingRepository userEmbeddings = mock(UserEmbeddingRepository.class);
    private final EventEmbeddingRepository eventEmbeddings = mock(EventEmbeddingRepository.class);
    private final GetRecommendedEventsHandler handler = new GetRecommendedEventsHandler(
            repository, mapper, eventFinder, userEmbeddings, eventEmbeddings, new PublishedEventSpecifications());

    @Test
    void handle_preservesEmbeddingCandidateOrder() {
        User user = user();
        Event first = event(10L);
        Event second = event(20L);
        EventResponse firstResponse = mock(EventResponse.class);
        EventResponse secondResponse = mock(EventResponse.class);
        when(eventFinder.findCurrentUser("user@loopin.test")).thenReturn(user);
        when(userEmbeddings.existsByUserId(7L)).thenReturn(true);
        when(eventEmbeddings.findSimilarEventsForUser(7L, 2))
                .thenReturn(List.of(new EventCandidate(20L, 0.9), new EventCandidate(10L, 0.8)));
        when(repository.findPublishedByIdInWithInterests(List.of(20L, 10L))).thenReturn(List.of(first, second));
        when(mapper.toResponse(second)).thenReturn(secondResponse);
        when(mapper.toResponse(first)).thenReturn(firstResponse);

        assertEquals(List.of(secondResponse, firstResponse),
                handler.handle(new GetRecommendedEventsQuery("user@loopin.test", 2)));
    }

    @Test
    void handle_fallsBackWhenEmbeddingOrCandidatesAreUnavailable() {
        User user = user();
        Event fallback = event(30L);
        EventResponse response = mock(EventResponse.class);
        when(eventFinder.findCurrentUser("user@loopin.test")).thenReturn(user);
        when(userEmbeddings.existsByUserId(7L)).thenReturn(false);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fallback)));
        when(repository.findPublishedByIdInWithInterests(List.of(30L))).thenReturn(List.of(fallback));
        when(mapper.toResponse(fallback)).thenReturn(response);

        assertEquals(List.of(response), handler.handle(new GetRecommendedEventsQuery("user@loopin.test", 3)));
        verify(eventEmbeddings, org.mockito.Mockito.never()).findSimilarEventsForUser(7L, 3);
    }

    @Test
    void handle_fallsBackWhenNoRecommendationCandidatesAreReturned() {
        User user = user();
        Event fallback = event(40L);
        EventResponse response = mock(EventResponse.class);
        when(eventFinder.findCurrentUser("user@loopin.test")).thenReturn(user);
        when(userEmbeddings.existsByUserId(7L)).thenReturn(true);
        when(eventEmbeddings.findSimilarEventsForUser(7L, 1)).thenReturn(List.of());
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fallback)));
        when(repository.findPublishedByIdInWithInterests(List.of(40L))).thenReturn(List.of(fallback));
        when(mapper.toResponse(fallback)).thenReturn(response);

        assertEquals(List.of(response), handler.handle(new GetRecommendedEventsQuery("user@loopin.test", 1)));
    }

    private User user() {
        User user = new User("user@loopin.test", "User", null);
        user.setId(7L);
        return user;
    }

    private Event event(Long id) {
        Event event = new Event();
        event.setId(id);
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }
}
