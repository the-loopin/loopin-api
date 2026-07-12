package com.loopin.api;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.chat.entity.GroupMessage;
import com.loopin.api.chat.repository.GroupMessageRepository;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventInterest;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupJoinRequest;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.interests.entity.Interest;
import com.loopin.api.interests.repository.InterestRepository;
import com.loopin.api.recommendation.event.EventCandidate;
import com.loopin.api.recommendation.event.EventEmbeddingRepository;
import com.loopin.api.recommendation.user.UserEmbeddingRepository;
import com.loopin.api.support.AbstractIntegrationTest;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgreSqlPersistenceIntegrationTest extends AbstractIntegrationTest {

    private static final String MODEL = "test-embedding-model";
    private static final String DEFAULT_MODEL = "intfloat/multilingual-e5-small";

    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private InterestRepository interestRepository;
    @Autowired private EventInterestRepository eventInterestRepository;
    @Autowired private EventGroupRepository eventGroupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private GroupJoinRequestRepository groupJoinRequestRepository;
    @Autowired private GroupMessageRepository groupMessageRepository;
    @Autowired private EventEmbeddingRepository eventEmbeddingRepository;
    @Autowired private UserEmbeddingRepository userEmbeddingRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    @Test
    void publishedEventQueriesKeepOwnerAndInterestsAndExcludeCancelledAndDeletedEvents() {
        User owner = saveUser("owner-events@example.test");
        Interest interest = interestRepository.saveAndFlush(interest("Technology", "technology-pg"));
        Event published = saveEvent("Published", owner, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        eventInterestRepository.saveAndFlush(new EventInterest(published, interest));

        Event cancelled = saveEvent("Cancelled", owner, EventStatus.CANCELLED, LocalDateTime.now().plusDays(2));
        Event deleted = saveEvent("Deleted", owner, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        deleted.markAsDeleted();
        eventRepository.saveAndFlush(deleted);
        entityManager.clear();

        List<Event> results = eventRepository.findPublishedByIdInWithInterests(
                List.of(published.getId(), cancelled.getId(), deleted.getId())
        );

        assertThat(results).singleElement().satisfies(event -> {
            assertThat(event.getId()).isEqualTo(published.getId());
            assertThat(event.getOwner().getId()).isEqualTo(owner.getId());
            assertThat(event.getInterests()).extracting(EventInterest::getInterest)
                    .extracting(Interest::getSlug).containsExactly("technology-pg");
        });
    }

    @Test
    void groupMembershipAndJoinRequestQueriesUsePersistedRelationshipsAndStatuses() {
        User admin = saveUser("admin-groups@example.test");
        User member = saveUser("member-groups@example.test");
        User pendingUser = saveUser("pending-groups@example.test");
        Event event = saveEvent("Group event", admin, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        EventGroup group = saveGroup(event, admin);

        groupMemberRepository.saveAndFlush(new GroupMember(group, member, null));
        groupJoinRequestRepository.saveAndFlush(new GroupJoinRequest(group, pendingUser, RequestStatus.PENDING, "Please add me"));
        groupJoinRequestRepository.saveAndFlush(new GroupJoinRequest(group, member, RequestStatus.ACCEPTED, "Already accepted"));
        entityManager.clear();

        assertThat(eventGroupRepository.findByEventIdAndStatusNot(event.getId(), GroupStatus.ARCHIVED))
                .extracting(EventGroup::getId).containsExactly(group.getId());
        assertThat(groupMemberRepository.existsByGroupIdAndUserId(group.getId(), member.getId())).isTrue();
        assertThat(groupMemberRepository.findDistinctActiveUsersByEventId(event.getId()))
                .extracting(User::getId).containsExactly(member.getId());
        assertThat(groupJoinRequestRepository.findByGroupIdAndStatus(group.getId(), RequestStatus.PENDING))
                .extracting(request -> request.getUser().getId()).containsExactly(pendingUser.getId());
        assertThat(groupJoinRequestRepository.existsByGroupIdAndUserIdAndStatus(
                group.getId(), pendingUser.getId(), RequestStatus.PENDING
        )).isTrue();
    }

    @Test
    void chatPaginationIsChronologicalAndRepositoryCleanupAllowsGroupDeletion() {
        User admin = saveUser("admin-chat@example.test");
        EventGroup group = saveGroup(
                saveEvent("Chat event", admin, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2)), admin
        );
        GroupMessage first = groupMessageRepository.saveAndFlush(new GroupMessage(group, admin, "first"));
        GroupMessage second = groupMessageRepository.saveAndFlush(new GroupMessage(group, admin, "second"));
        GroupMessage third = groupMessageRepository.saveAndFlush(new GroupMessage(group, admin, "third"));
        jdbcTemplate.update("update group_messages set created_at = ? where id = ?", LocalDateTime.now().minusMinutes(3), first.getId());
        jdbcTemplate.update("update group_messages set created_at = ? where id = ?", LocalDateTime.now().minusMinutes(2), second.getId());
        jdbcTemplate.update("update group_messages set created_at = ? where id = ?", LocalDateTime.now().minusMinutes(1), third.getId());
        entityManager.clear();

        assertThat(groupMessageRepository.findByGroupIdOrderByCreatedAtAsc(group.getId(), PageRequest.of(0, 2)))
                .extracting(GroupMessage::getMessageText).containsExactly("first", "second");

        group.setStatus(GroupStatus.ARCHIVED);
        eventGroupRepository.saveAndFlush(group);
        assertThat(groupMessageRepository.count()).isEqualTo(3);

        groupMessageRepository.deleteByGroupId(group.getId());
        entityManager.flush();
        eventGroupRepository.deleteById(group.getId());
        entityManager.flush();
        assertThat(eventGroupRepository.findById(group.getId())).isEmpty();
    }

    @Test
    void postgresUniqueKeysRejectInvalidPersistence() {
        User user = saveUser("unique-user@example.test");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into users (public_id, created_at, updated_at, name, email, role, is_active) values (gen_random_uuid(), now(), now(), ?, ?, ?, ?)",
                "Duplicate", user.getEmail(), Role.USER.name(), true
        )).isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(throwable -> assertPostgreSqlConstraint(
                        throwable, "23505", "uk_users_email"
                ));
    }

    @Test
    void postgresForeignKeysRejectInvalidPersistence() {
        User user = saveUser("foreign-key-user@example.test");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into event_groups (public_id, created_at, updated_at, event_id, admin_id, max_members) values (gen_random_uuid(), now(), now(), ?, ?, ?)",
                Long.MAX_VALUE, user.getId(), 2
        )).isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(throwable -> assertPostgreSqlConstraint(
                        throwable, "23503", "fk_event_groups_event_id"
                ));
    }

    @Test
    void pgvectorUpsertsStore384DimensionsAndSimilarityQueriesFilterAndOrderResults() {
        User owner = saveUser("owner-vectors@example.test");
        User recommendationUser = saveUser("user-vectors@example.test");
        Event nearest = saveEvent("Nearest", owner, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        Event next = saveEvent("Next", owner, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        Event wrongModel = saveEvent("Wrong model", owner, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        Event unpublished = saveEvent("Unpublished", owner, EventStatus.DRAFT, LocalDateTime.now().plusDays(2));
        Event expired = saveEvent("Expired", owner, EventStatus.PUBLISHED, LocalDateTime.now().minusDays(1));
        Event deleted = saveEvent("Deleted", owner, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2));
        deleted.markAsDeleted();
        eventRepository.saveAndFlush(deleted);

        eventEmbeddingRepository.upsert(nearest.getId(), vector(1, 0), MODEL, "nearest-v1");
        eventEmbeddingRepository.upsert(next.getId(), vector(0.8, 0.2), MODEL, "next-v1");
        eventEmbeddingRepository.upsert(wrongModel.getId(), vector(1, 0), "other-model", "other-v1");
        eventEmbeddingRepository.upsert(unpublished.getId(), vector(1, 0), MODEL, "draft-v1");
        eventEmbeddingRepository.upsert(expired.getId(), vector(1, 0), MODEL, "expired-v1");
        eventEmbeddingRepository.upsert(deleted.getId(), vector(1, 0), MODEL, "deleted-v1");
        eventEmbeddingRepository.upsert(nearest.getId(), vector(0.99, 0.01), MODEL, "nearest-v2");

        assertThat(jdbcTemplate.queryForObject(
                "select vector_dims(embedding) from event_embeddings where event_id = ?", Integer.class, nearest.getId()
        )).isEqualTo(384);
        assertThat(jdbcTemplate.queryForObject(
                "select source_text_hash from event_embeddings where event_id = ?", String.class, nearest.getId()
        )).isEqualTo("nearest-v2");

        List<EventCandidate> candidates = eventEmbeddingRepository.findSimilarEvents(vector(1, 0), MODEL, 10);
        assertThat(candidates).extracting(EventCandidate::eventId).containsExactly(nearest.getId(), next.getId());
        assertThat(candidates).extracting(EventCandidate::retrievalScore).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(eventEmbeddingRepository.findSimilarEvents(vector(1, 0), MODEL, 1))
                .extracting(EventCandidate::eventId).containsExactly(nearest.getId());

        userEmbeddingRepository.upsert(recommendationUser.getId(), vector(1, 0), MODEL, "user-v1");
        userEmbeddingRepository.upsert(recommendationUser.getId(), vector(0.99, 0.01), MODEL, "user-v2");
        assertThat(userEmbeddingRepository.existsByUserIdAndModel(recommendationUser.getId(), MODEL)).isTrue();
        assertThat(userEmbeddingRepository.existsByUserIdAndModel(recommendationUser.getId(), "other-model")).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "select vector_dims(embedding) from user_interest_embeddings where user_id = ?", Integer.class, recommendationUser.getId()
        )).isEqualTo(384);
        assertThat(eventEmbeddingRepository.findSimilarEventsForUser(recommendationUser.getId(), MODEL, 10))
                .extracting(EventCandidate::eventId).containsExactly(nearest.getId(), next.getId());

        eventEmbeddingRepository.upsert(nearest.getId(), vector(1, 0), DEFAULT_MODEL, "nearest-default");
        userEmbeddingRepository.upsert(recommendationUser.getId(), vector(1, 0), DEFAULT_MODEL, "user-default");
        assertThat(eventEmbeddingRepository.findSimilarEvents(vector(1, 0), 1))
                .extracting(EventCandidate::eventId).containsExactly(nearest.getId());
        assertThat(eventEmbeddingRepository.findSimilarEventsForUser(recommendationUser.getId(), 1))
                .extracting(EventCandidate::eventId).containsExactly(nearest.getId());
    }

    private User saveUser(String email) {
        User user = new User(email, email, null);
        user.setRole(Role.USER);
        return userRepository.saveAndFlush(user);
    }

    private Event saveEvent(String title, User owner, EventStatus status, LocalDateTime endDateTime) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(title + " description");
        event.setType(EventType.EVENT);
        event.setCategory(EventCategory.TECH);
        event.setCity("Baku");
        event.setStartDateTime(endDateTime.minusHours(2));
        event.setEndDateTime(endDateTime);
        event.setIsFree(true);
        event.setPrice(BigDecimal.ZERO);
        event.setOrganizerName("Loopin");
        event.setStatus(status);
        event.setOwner(owner);
        return eventRepository.saveAndFlush(event);
    }

    private EventGroup saveGroup(Event event, User admin) {
        return eventGroupRepository.saveAndFlush(new EventGroup(
                event, admin, "Persistence group", GroupSizeType.FOUR, 4, GroupStatus.OPEN, "note"
        ));
    }

    private Interest interest(String name, String slug) {
        Interest interest = new Interest();
        interest.setName(name);
        interest.setSlug(slug);
        interest.setCategory("Professional");
        return interest;
    }

    private List<Double> vector(double first, double second) {
        List<Double> vector = new ArrayList<>(384);
        vector.add(first);
        vector.add(second);
        while (vector.size() < 384) {
            vector.add(0.0);
        }
        return vector;
    }

    private void assertPostgreSqlConstraint(Throwable throwable, String sqlState, String constraintName) {
        PSQLException postgresException = findPostgreSqlException(throwable);
        assertThat((Object) postgresException).isNotNull();
        assertThat(postgresException.getSQLState()).isEqualTo(sqlState);
        assertThat(postgresException.getServerErrorMessage()).isNotNull();
        assertThat(postgresException.getServerErrorMessage().getConstraint()).isEqualTo(constraintName);
    }

    private PSQLException findPostgreSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof PSQLException postgresException) {
                return postgresException;
            }
            current = current.getCause();
        }
        return null;
    }
}
