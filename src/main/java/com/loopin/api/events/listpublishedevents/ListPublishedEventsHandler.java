package com.loopin.api.events.listpublishedevents;

import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.specification.PublishedEventSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListPublishedEventsHandler {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final PublishedEventSpecifications specifications;

    /**
     * The query record includes every filter and the complete pageable value, giving the cache a
     * stable key for equivalent public-list requests.
     */
    @Cacheable(value = "publishedEvents", key = "#query")
    @Transactional(readOnly = true)
    public Page<EventResponse> handle(ListPublishedEventsQuery query) {
        log.info("Fetching published events with filters - type: {}, category: {}, city: {}, isFree: {}, search: {}",
                query.type(), query.category(), query.city(), query.isFree(), query.search());

        Page<Event> eventPage = eventRepository.findAll(
                specifications.forPublishedListing(query),
                query.pageable()
        );
        List<EventResponse> responses = loadWithInterestsInPageOrder(eventPage.getContent()).stream()
                .map(eventMapper::toResponse)
                .toList();

        return new PageImpl<>(responses, query.pageable(), eventPage.getTotalElements());
    }

    private List<Event> loadWithInterestsInPageOrder(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Event> eventsById = eventRepository.findPublishedByIdInWithInterests(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        return eventIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
