package com.loopin.api.service.implementation;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.dto.event.request.EventCreateRequest;
import com.loopin.api.dto.event.request.EventUpdateRequest;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.EventInterest;
import com.loopin.api.entity.Interest;
import com.loopin.api.entity.User;
import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.mapper.EventMapper;
import com.loopin.api.repository.EventInterestRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.InterestRepository;
import com.loopin.api.repository.UserRepository;
import com.loopin.api.recommendation.EventEmbeddingService;
import com.loopin.api.service.abstraction.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final InterestRepository interestRepository;
    private final EventInterestRepository eventInterestRepository;
    private final EventEmbeddingService eventEmbeddingService;
    private final com.loopin.api.recommendation.UserEmbeddingRepository userEmbeddingRepository;
    private final com.loopin.api.recommendation.EventEmbeddingRepository eventEmbeddingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getPublishedEvents(
            EventType type,
            EventCategory category,
            String city,
            Boolean isFree,
            String search,
            LocalDate startDate,
            LocalDate endDate
    ) {
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

        return eventRepository.findAll(specification)
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getPublishedEventById(UUID id) {
        Event event = eventRepository.findOne(
                Specification.where(notDeleted())
                        .and(hasStatus(EventStatus.PUBLISHED))
                        .and(hasPublicId(id))
        ).orElseThrow(() -> new NoSuchElementException("Published event not found with id: " + id));

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getRecommendedEvents(String currentUsername, int limit) {
        User currentUser = findCurrentUser(currentUsername);

        if (userEmbeddingRepository.existsByUserId(currentUser.getId())) {
            List<com.loopin.api.recommendation.EventCandidate> candidates =
                    eventEmbeddingRepository.findSimilarEventsForUser(currentUser.getId(), limit);

            if (!candidates.isEmpty()) {
                List<Long> eventIds = candidates.stream()
                        .map(com.loopin.api.recommendation.EventCandidate::eventId)
                        .toList();

                List<Event> events = eventRepository.findAllById(eventIds);

                Map<Long, Event> eventMap = events.stream()
                        .collect(Collectors.toMap(Event::getId, Function.identity()));

                return candidates.stream()
                        .map(candidate -> eventMap.get(candidate.eventId()))
                        .filter(java.util.Objects::nonNull)
                        .map(eventMapper::toResponse)
                        .toList();
            }
        }

        // Fallback: Fetch recently published events that are not deleted and haven't ended yet
        Specification<Event> specification = Specification
                .where(notDeleted())
                .and(hasStatus(EventStatus.PUBLISHED))
                .and((root, query, criteriaBuilder) ->
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endDateTime"), LocalDateTime.now()));

        return eventRepository.findAll(
                specification,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        )
                .stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse createEvent(EventCreateRequest request, String currentUsername) {
        User currentUser = findCurrentUser(currentUsername);
        validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        validatePrice(request.getIsFree(), request.getPrice());
        validateEventDoesNotAlreadyExist(request);

        Event event = eventMapper.toEntity(request);
        event.setOwner(currentUser);
        Event savedEvent = eventRepository.saveAndFlush(event);
        replaceEventInterests(savedEvent, request.getInterestIds());
        eventEmbeddingService.indexEvent(savedEvent);

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(UUID id, EventUpdateRequest request, String currentUsername) {
        User currentUser = findCurrentUser(currentUsername);
        validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        validatePrice(request.getIsFree(), request.getPrice());

        Event event = findActiveEventById(id);
        validateOwnerOrAdmin(event, currentUser);
        eventMapper.updateEntity(event, request);
        replaceEventInterests(event, request.getInterestIds());

        Event savedEvent = eventRepository.save(event);
        eventEmbeddingService.indexEvent(savedEvent);
        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional
    public void deleteEvent(UUID id, String currentUsername) {
        User currentUser = findCurrentUser(currentUsername);
        Event event = findActiveEventById(id);
        validateOwnerOrAdmin(event, currentUser);
        eventInterestRepository.deleteByEvent_Id(event.getId());
        event.markAsDeleted();
        eventRepository.save(event);
    }

    private void replaceEventInterests(Event event, List<UUID> interestIds) {
        List<UUID> requestedInterestIds = interestIds == null ? List.of() : interestIds;
        Set<UUID> uniqueInterestIds = new LinkedHashSet<>(requestedInterestIds);

        if (uniqueInterestIds.size() != requestedInterestIds.size()) {
            throw new IllegalArgumentException("Duplicate interests are not allowed.");
        }

        Map<UUID, Interest> interestsByPublicId = findInterestsByPublicId(uniqueInterestIds);

        if (event.getId() != null) {
            eventInterestRepository.deleteByEvent_Id(event.getId());
            eventInterestRepository.flush();
        }

        List<EventInterest> eventInterests = requestedInterestIds.stream()
                .map(interestId -> new EventInterest(event, interestsByPublicId.get(interestId)))
                .toList();

        List<EventInterest> savedEventInterests = eventInterestRepository.saveAll(eventInterests);
        event.setInterests(new LinkedHashSet<>(savedEventInterests));
    }

    private Map<UUID, Interest> findInterestsByPublicId(Set<UUID> publicIds) {
        if (publicIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Interest> interestsByPublicId = interestRepository.findByPublicIdInAndDeletedAtIsNull(publicIds)
                .stream()
                .collect(Collectors.toMap(Interest::getPublicId, Function.identity()));

        if (interestsByPublicId.size() != publicIds.size()) {
            throw new NoSuchElementException("One or more interests were not found.");
        }

        return interestsByPublicId;
    }

    private User findCurrentUser(String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }

        return userRepository.findByEmailAndDeletedAtIsNull(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));
    }

    private void validateOwnerOrAdmin(Event event, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (event.getOwner() != null && event.getOwner().getId().equals(currentUser.getId())) {
            return;
        }

        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Only the event owner or an admin can modify this event");
    }

    private Event findActiveEventById(UUID id) {
        return eventRepository.findOne(
                Specification.where(notDeleted()).and(hasPublicId(id))
        ).orElseThrow(() -> new NoSuchElementException("Event not found with id: " + id));
    }

    private void validateDateRange(java.time.LocalDateTime startDateTime, java.time.LocalDateTime endDateTime) {
        if (!endDateTime.isAfter(startDateTime)) {
            throw new IllegalArgumentException("End date and time must be after start date and time");
        }
    }

    private void validatePrice(Boolean isFree, BigDecimal price) {
        if (Boolean.TRUE.equals(isFree)) {
            if (price != null && price.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("Free events must have price 0 or null");
            }
            return;
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Paid events must have price greater than 0");
        }
    }

    private void validateEventDoesNotAlreadyExist(EventCreateRequest request) {
        boolean exists = eventRepository.exists(
                Specification.where(notDeleted())
                        .and(titleEqualsIgnoreCase(request.getTitle()))
                        .and(cityEqualsIgnoreCase(request.getCity()))
                        .and(startsAt(request.getStartDateTime()))
        );

        if (exists) {
            throw new DuplicateResourceException("Event already exists with same title, city, and start date time");
        }
    }

    private Specification<Event> alwaysTrue() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    private Specification<Event> titleEqualsIgnoreCase(String title) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("title")),
                        title.toLowerCase()
                );
    }

    private Specification<Event> cityEqualsIgnoreCase(String city) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("city")),
                        city.toLowerCase()
                );
    }

    private Specification<Event> startsAt(LocalDateTime startDateTime) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("startDateTime"), startDateTime);
    }

    private Specification<Event> hasPublicId(UUID id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("publicId"), id);
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


