package com.loopin.api.events.service;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventInterest;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.interests.entity.Interest;
import com.loopin.api.users.entity.User;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.common.exception.UnauthorizedException;
import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.recommendation.event.EventCandidate;
import com.loopin.api.recommendation.event.EventEmbeddingRepository;
import com.loopin.api.recommendation.user.UserEmbeddingRepository;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.interests.repository.InterestRepository;
import com.loopin.api.users.repository.UserRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import com.loopin.api.events.service.EventService;
import com.loopin.api.notifications.service.NotificationService;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.moderation.ContentModerationService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final EventGroupRepository eventGroupRepository;
    private final InterestRepository interestRepository;
    private final EventInterestRepository eventInterestRepository;
    private final EventEmbeddingService eventEmbeddingService;
    private final UserEmbeddingRepository userEmbeddingRepository;
    private final EventEmbeddingRepository eventEmbeddingRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;
    private final ContentModerationService contentModerationService;

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
    @Cacheable(value = "eventById", key = "#id")
    @Transactional(readOnly = true)
    public EventResponse getPublishedEventById(UUID id) {
        Event event = eventRepository.findPublishedByPublicIdWithInterests(id)
                .orElseThrow(() -> new NoSuchElementException("Published event not found with id: " + id));

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getRecommendedEvents(String currentUsername, int limit) {
        log.info("Fetching recommended events for user: {}, limit: {}", currentUsername, limit);
        User currentUser = findCurrentUser(currentUsername);

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
    @CacheEvict(value = "publishedEvents", allEntries = true)
    @Transactional
    public EventResponse createEvent(EventCreateRequest request, String currentUsername) {
        User currentUser = findCurrentUser(currentUsername);
        validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        validatePrice(request.getIsFree(), request.getPrice());
        validateEventDoesNotAlreadyExist(request);

        Event event = eventMapper.toEntity(request);
        event.setOwner(currentUser);
        applyModerationStatus(event, request.getTitle(), request.getDescription());
        Event savedEvent = eventRepository.saveAndFlush(event);
        replaceEventInterests(savedEvent, request.getInterestIds());
        eventEmbeddingService.indexEvent(savedEvent);
        notificationService.create(new NotificationCommand(
                currentUser,
                NotificationType.EVENT_UPDATE,
                "Event created",
                "Your event \"" + savedEvent.getTitle() + "\" was created.",
                NotificationReferenceType.EVENT,
                savedEvent.getPublicId()));

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "publishedEvents", allEntries = true),
        @CacheEvict(value = "eventById", key = "#id")
    })
    @Transactional
    public EventResponse updateEvent(UUID id, EventUpdateRequest request, String currentUsername) {
        User currentUser = findCurrentUser(currentUsername);
        validateDateRange(request.getStartDateTime(), request.getEndDateTime());
        validatePrice(request.getIsFree(), request.getPrice());

        Event event = findActiveEventById(id);
        validateOwnerOrAdmin(event, currentUser);
        EventStatus previousStatus = event.getStatus();
        eventMapper.updateEntity(event, request);
        applyModerationStatus(event, request.getTitle(), request.getDescription());
        replaceEventInterests(event, request.getInterestIds());

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
        User currentUser = findCurrentUser(currentUsername);
        Event event = findActiveEventById(id);
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

    private void replaceEventInterests(Event event, List<UUID> interestIds) {
        List<UUID> requestedInterestIds = interestIds == null ? List.of() : interestIds;
        Set<UUID> uniqueInterestIds = new LinkedHashSet<>(requestedInterestIds);
    
        if (uniqueInterestIds.size() != requestedInterestIds.size()) {
            throw new IllegalArgumentException("Duplicate interests are not allowed.");
        }
    
        Map<UUID, Interest> interestsByPublicId = findInterestsByPublicId(uniqueInterestIds);
    
        Set<EventInterest> newInterests = uniqueInterestIds.stream()
                .map(interestId -> new EventInterest(event, interestsByPublicId.get(interestId)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    
        if (event.getInterests() == null) {
            event.setInterests(new LinkedHashSet<>());
        }
    
        event.getInterests().clear();
        event.getInterests().addAll(newInterests);
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

    private User findCurrentUser(String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new UnauthorizedException("Authentication is required");
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

        throw new ForbiddenAccessException("Only the event owner or an admin can modify this event");
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

    private void applyModerationStatus(Event event, String title, String description) {
        if (!contentModerationService.moderate(title, description).isApproved()) {
            event.setModerationStatus(ContentModerationStatus.PENDING_REVIEW);
            event.setModerationRejectionReason(null);
            // Pending content must remain outside public event queries until an
            // administrator makes an explicit moderation decision.
            event.setStatus(EventStatus.DRAFT);
            return;
        }

        event.setModerationStatus(ContentModerationStatus.APPROVED);
        event.setModerationRejectionReason(null);
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


