package com.loopin.api.integration;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.events.repository.projection.LoopedEventCountProjection;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import com.loopin.api.support.AbstractIntegrationTest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserLoopedEventRepositoryIntegrationTest
    extends AbstractIntegrationTest {

    @Autowired
    private UserLoopedEventRepository loopedEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void insertIfAbsent_DuplicateUserEvent_CreatesOnlyOneRow() {
        User user = saveUser("participant@example.test");
        Event event = saveEvent("Event", saveUser("owner@example.test"));

        LocalDateTime createdAt = LocalDateTime.now().withNano(0);

        int firstInsert = loopedEventRepository.insertIfAbsent(
            UUID.randomUUID(),
            createdAt,
            user.getId(),
            event.getId()
        );

        int duplicateInsert = loopedEventRepository.insertIfAbsent(
            UUID.randomUUID(),
            createdAt.plusSeconds(1),
            user.getId(),
            event.getId()
        );

        entityManager.flush();
        entityManager.clear();

        assertEquals(1, firstInsert);
        assertEquals(0, duplicateInsert);
        assertEquals(
            1L,
            loopedEventRepository.countByEventId(event.getId())
        );
    }

    @Test
    void countByEventIds_ReturnsCountsInSingleGroupedQuery() {
        User firstUser = saveUser("first@example.test");
        User secondUser = saveUser("second@example.test");
        User owner = saveUser("owner@example.test");

        Event firstEvent = saveEvent("First event", owner);
        Event secondEvent = saveEvent("Second event", owner);

        LocalDateTime now = LocalDateTime.now().withNano(0);

        loopedEventRepository.insertIfAbsent(
            UUID.randomUUID(),
            now,
            firstUser.getId(),
            firstEvent.getId()
        );

        loopedEventRepository.insertIfAbsent(
            UUID.randomUUID(),
            now.plusSeconds(1),
            secondUser.getId(),
            firstEvent.getId()
        );

        loopedEventRepository.insertIfAbsent(
            UUID.randomUUID(),
            now.plusSeconds(2),
            firstUser.getId(),
            secondEvent.getId()
        );

        List<LoopedEventCountProjection> projections =
            loopedEventRepository.countByEventIds(
                List.of(firstEvent.getId(), secondEvent.getId())
            );

        Map<Long, Long> counts = projections.stream()
            .collect(Collectors.toMap(
                LoopedEventCountProjection::getEventId,
                LoopedEventCountProjection::getLoopedCount
            ));

        assertEquals(2L, counts.get(firstEvent.getId()));
        assertEquals(1L, counts.get(secondEvent.getId()));
    }

    @Test
    void findPageByUserId_ReturnsNewestLoopInFirst() {
        User participant = saveUser("participant@example.test");
        User owner = saveUser("owner@example.test");

        Event olderEvent = saveEvent("Older event", owner);
        Event newerEvent = saveEvent("Newer event", owner);

        LocalDateTime now = LocalDateTime.now().withNano(0);

        loopedEventRepository.insertIfAbsent(
            UUID.randomUUID(),
            now.minusMinutes(5),
            participant.getId(),
            olderEvent.getId()
        );

        loopedEventRepository.insertIfAbsent(
            UUID.randomUUID(),
            now,
            participant.getId(),
            newerEvent.getId()
        );

        entityManager.flush();
        entityManager.clear();

        var page = loopedEventRepository.findPageByUserId(
            participant.getId(),
            PageRequest.of(0, 10)
        );

        assertEquals(2, page.getContent().size());
        assertEquals(
            newerEvent.getPublicId(),
            page.getContent().get(0).getEvent().getPublicId()
        );
        assertEquals(
            olderEvent.getPublicId(),
            page.getContent().get(1).getEvent().getPublicId()
        );
    }

    @Test
    void deleteByUserIdAndEventPublicId_RepeatedDeleteIsIdempotent() {
        User participant = saveUser("participant@example.test");
        User owner = saveUser("owner@example.test");
        Event event = saveEvent("Event", owner);

        loopedEventRepository.insertIfAbsent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            participant.getId(),
            event.getId()
        );

        int firstDelete =
            loopedEventRepository.deleteByUserIdAndEventPublicId(
                participant.getId(),
                event.getPublicId()
            );

        int secondDelete =
            loopedEventRepository.deleteByUserIdAndEventPublicId(
                participant.getId(),
                event.getPublicId()
            );

        assertEquals(1, firstDelete);
        assertEquals(0, secondDelete);
        assertEquals(
            0L,
            loopedEventRepository.countByEventId(event.getId())
        );
    }

    private User saveUser(String email) {
        User user = new User(email, email, null);
        user.setRole(Role.USER);
        return userRepository.saveAndFlush(user);
    }

    private Event saveEvent(String title, User owner) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("Description");
        event.setType(EventType.EVENT);
        event.setCategory(EventCategory.TECH);
        event.setCity("Baku");
        event.setStartDateTime(
            LocalDateTime.now().plusDays(1)
        );
        event.setEndDateTime(
            LocalDateTime.now().plusDays(2)
        );
        event.setIsFree(true);
        event.setPrice(BigDecimal.ZERO);
        event.setOrganizerName("Loopin");
        event.setStatus(EventStatus.PUBLISHED);
        event.setModerationStatus(
            ContentModerationStatus.APPROVED
        );
        event.setOwner(owner);

        return eventRepository.saveAndFlush(event);
    }
}
