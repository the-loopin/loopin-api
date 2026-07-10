package com.loopin.api.events.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.recommendation.event.EventCandidate;
import com.loopin.api.recommendation.event.EventEmbeddingRepository;
import com.loopin.api.recommendation.user.UserEmbeddingRepository;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.interest.EventInterestManager;
import com.loopin.api.events.shared.moderation.EventModerationManager;
import com.loopin.api.events.shared.validation.EventValidator;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import com.loopin.api.events.service.EventService;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.notifications.service.NotificationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventGroupRepository eventGroupRepository;
    private final EventInterestRepository eventInterestRepository;
    private final EventEmbeddingService eventEmbeddingService;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final EventEmbeddingRepository eventEmbeddingRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;
    private final EventFinder eventFinder;
    private final EventValidator eventValidator;
    private final EventInterestManager eventInterestManager;
    private final EventModerationManager eventModerationManager;

    @Override
    @Cacheable(value = "publishedEvents", key = "{#type, #category, #city, #isFree, #search, #startDate, #endDate, #pageable}")
    @Transactional(readOnly = true)
    public Page<EventResponse> getPublishedEvents(
            EventType type,
            EventCategory category,
            String city,
            Boolean isFree,
            String search,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Fetching published events with filters - type: {}, category: {}, city: {}, isFree: {}, search: {}",
                type, category, city, isFree, search);

        Specification<Event> specification = Specification
                .where(notDeleted())
                .and(hasStatus(EventStatus.PUBLISHED))
                .and(hasType(type))
                .and(hasCategory(category))
                .and(cityContains(city))
                .and(hasIsFree(isFree))
                .and(searchInTitleOrDescription(search))
                .and(startsOnOrAfter(startDate))
                .and(startsOnOrBefore(endDate));

        Page<Event> eventPage = eventRepository.findAll(specification, pageable);
        List<EventResponse> responses = fetchPublishedEventsWithInterestsInOrder(eventPage.getContent())
                .stream()
                .map(eventMapper::toResponse)
                .toList();

        return new PageImpl<>(responses, pageable, eventPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getRecommendedEvents(String currentUsername, int limit) {
        log.info("Fetching recommended events for user: {}, limit: {}", currentUsername, limit);
        User currentUser = eventFinder.findCurrentUser(currentUsername);

        if (userEmbeddingRepository.existsByUserId(currentUser.getId())) {
            List<EventCandidate> candidates =
                    eventEmbeddingRepository.findSimilarEventsForUser(currentUser.getId(), limit);

            if (!candidates.isEmpty()) {
                List<Long> eventIds = candidates.stream()
                        .map(EventCandidate::eventId)
                        .toList();

                List<Event> events = eventRepository.findPublishedByIdInWithInterests(eventIds);
                Map<Long, Event> eventsById = events.stream()
                        .collect(Collectors.toMap(Event::getId, Function.identity()));

                return eventIds.stream()
                        .map(eventsById::get)
                        .filter(Objects::nonNull)
                        .map(eventMapper::toResponse)
                        .toList();
            }
        }

        log.debug("No user embedding found for user: {}, falling back to recent events", currentUsername);
        // Fallback: Fetch recently published events that are not deleted and haven't ended yet
        Specification<Event> specification = Specification
                .where(notDeleted())
                .and(hasStatus(EventStatus.PUBLISHED))
                .and((root, query, criteriaBuilder) ->
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endDateTime"), LocalDateTime.now()));

        Page<Event> fallbackPage = eventRepository.findAll(
                specification,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return fetchPublishedEventsWithInterestsInOrder(fallbackPage.getContent())
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "publishedEvents", allEntries = true),
        @CacheEvict(value = "eventById", key = "#id")
    })
    @Transactional
    public EventResponse updateEvent(UUID id, EventUpdateRequest request, String currentUsername) {
        User currentUser = eventFinder.findCurrentUser(currentUsername);
        eventValidator.validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        eventValidator.validatePrice(request.getIsFree(), request.getPrice());

        Event event = eventFinder.findActiveEventById(id);
        validateOwnerOrAdmin(event, currentUser);
        EventStatus previousStatus = event.getStatus();
        eventMapper.updateEntity(event, request);
        eventModerationManager.apply(event, request.getTitle(), request.getDescription());
        eventInterestManager.replace(event, request.getInterestIds());

        Event savedEvent = eventRepository.save(event);
        eventEmbeddingService.indexEvent(savedEvent);
        boolean cancelled = savedEvent.getStatus() == EventStatus.CANCELLED
                && previousStatus != EventStatus.CANCELLED;
        notifyEventMembers(
                savedEvent,
                cancelled ? "Event cancelled" : "Event updated",
                cancelled
                        ? "\"" + savedEvent.getTitle() + "\" has been cancelled."
                        : "\"" + savedEvent.getTitle() + "\" has been updated.");
        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "publishedEvents", allEntries = true),
        @CacheEvict(value = "eventById", key = "#id")
    })
    @Transactional
    public void deleteEvent(UUID id, String currentUsername) {
        User currentUser = eventFinder.findCurrentUser(currentUsername);
        Event event = eventFinder.findActiveEventById(id);
        validateOwnerOrAdmin(event, currentUser);
        notifyEventMembers(
                event,
                "Event cancelled",
                "\"" + event.getTitle() + "\" has been cancelled.");
        eventInterestRepository.deleteByEvent_Id(event.getId());
        archiveGroupsForEvent(event.getId());
        event.markAsDeleted();
        eventRepository.save(event);
    }

    private void notifyEventMembers(Event event, String title, String message) {
        List<NotificationCommand> commands = groupMemberRepository
                .findDistinctActiveUsersByEventId(event.getId())
                .stream()
                .map(recipient -> new NotificationCommand(
                        recipient,
                        NotificationType.EVENT_UPDATE,
                        title,
                        message,
                        NotificationReferenceType.EVENT,
                        event.getPublicId()))
                .toList();
        notificationService.createAll(commands);
    }

    private void archiveGroupsForEvent(Long eventId) {
        List<EventGroup> groups = eventGroupRepository.findByEventIdAndStatusNot(eventId, GroupStatus.ARCHIVED);
        groups.forEach(group -> {
            group.setStatus(GroupStatus.ARCHIVED);
            eventGroupRepository.save(group);
        });
    }

    private List<Event> fetchPublishedEventsWithInterestsInOrder(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Event> eventsWithInterestsById = eventRepository.findPublishedByIdInWithInterests(eventIds)
                .stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        return eventIds.stream()
                .map(eventsWithInterestsById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private void validateOwnerOrAdmin(Event event, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (event.getOwner() != null && event.getOwner().getId().equals(currentUser.getId())) {
            return;
        }

        throw new ForbiddenAccessException("Only the event owner or an admin can modify this event");
    }

    private Specification<Event> alwaysTrue() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    private Specification<Event> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    private Specification<Event> hasStatus(EventStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<Event> hasType(EventType type) {
        if (type == null) {
            return alwaysTrue();
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
    }

    private Specification<Event> hasCategory(EventCategory category) {
        if (category == null) {
            return alwaysTrue();
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category"), category);
    }

    private Specification<Event> cityContains(String city) {
        if (city == null || city.isBlank()) {
            return alwaysTrue();
        }

        String cityPattern = "%" + city.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), cityPattern);
    }

    private Specification<Event> hasIsFree(Boolean isFree) {
        if (isFree == null) {
            return alwaysTrue();
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isFree"), isFree);
    }

    private Specification<Event> searchInTitleOrDescription(String search) {
        if (search == null || search.isBlank()) {
            return alwaysTrue();
        }

        String searchPattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern)
        );
    }

    private Specification<Event> startsOnOrAfter(LocalDate startDate) {
        if (startDate == null) {
            return alwaysTrue();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("startDateTime"), startDate.atStartOfDay());
    }

    private Specification<Event> startsOnOrBefore(LocalDate endDate) {
        if (endDate == null) {
            return alwaysTrue();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(root.get("startDateTime"), endDate.plusDays(1).atStartOfDay());
    }
}


