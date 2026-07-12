package com.loopin.api.events.getrecommendedevents;

import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.specification.PublishedEventSpecifications;
import com.loopin.api.recommendation.event.EventCandidate;
import com.loopin.api.recommendation.event.EventEmbeddingRepository;
import com.loopin.api.recommendation.user.UserEmbeddingRepository;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRecommendedEventsHandler {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventFinder eventFinder;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final EventEmbeddingRepository eventEmbeddingRepository;
    private final PublishedEventSpecifications specifications;

    @Transactional(readOnly = true)
    public List<EventResponse> handle(GetRecommendedEventsQuery query) {
        log.info("Fetching recommended events limit={}", query.limit());
        User currentUser = eventFinder.findCurrentUser(query.currentUsername());

        if (userEmbeddingRepository.existsByUserId(currentUser.getId())) {
            List<EventCandidate> candidates = eventEmbeddingRepository
                    .findSimilarEventsForUser(currentUser.getId(), query.limit());
            if (!candidates.isEmpty()) {
                return mapCandidatesInRecommendationOrder(candidates);
            }
        }

        log.debug("No recommendation candidates found; falling back to recent events");
        Page<Event> fallbackPage = eventRepository.findAll(
                specifications.activePublishedAt(LocalDateTime.now()),
                PageRequest.of(0, query.limit(), Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return loadWithInterestsInOrder(fallbackPage.getContent()).stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    private List<EventResponse> mapCandidatesInRecommendationOrder(List<EventCandidate> candidates) {
        List<Long> eventIds = candidates.stream().map(EventCandidate::eventId).toList();
        Map<Long, Event> eventsById = eventRepository.findPublishedByIdInWithInterests(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        return eventIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .map(eventMapper::toResponse)
                .toList();
    }

    private List<Event> loadWithInterestsInOrder(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Event> eventsById = eventRepository.findPublishedByIdInWithInterests(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        return eventIds.stream().map(eventsById::get).filter(Objects::nonNull).toList();
    }
}
