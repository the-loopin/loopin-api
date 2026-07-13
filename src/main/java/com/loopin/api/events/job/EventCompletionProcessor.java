package com.loopin.api.events.job;

import com.loopin.api.common.cache.CacheNames;
import com.loopin.api.users.enums.BadgeType;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.entity.Event;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserBadge;
import com.loopin.api.groups.api.ArchivedGroupAwardRecipients;
import com.loopin.api.groups.api.GroupLifecycle;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.users.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventCompletionProcessor {

    private final EventRepository eventRepository;
    private final GroupLifecycle groupLifecycle;
    private final UserBadgeRepository userBadgeRepository;

    /**
     * The scheduled job only receives the internal id, while public-detail entries are keyed by
     * UUID. Completion is infrequent, so clearing that cache is safer than risking a stale detail.
     */
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PUBLISHED_EVENTS, allEntries = true),
            @CacheEvict(value = "eventById", allEntries = true)
    })
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventCompletionResult completeEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .filter(candidate -> candidate.getStatus() == EventStatus.PUBLISHED)
                .orElse(null);

        if (event == null) {
            return new EventCompletionResult(false, 0);
        }

        event.setStatus(EventStatus.COMPLETED);
        eventRepository.save(event);

        int archivedGroups = 0;
        List<ArchivedGroupAwardRecipients> groups = groupLifecycle.archiveActiveGroupsForEvent(eventId);
        for (ArchivedGroupAwardRecipients group : groups) {
            archivedGroups++;

            awardBadge(group.creator(), BadgeType.GROUP_CREATOR);
            awardAttendeeBadges(group.members());
        }

        return new EventCompletionResult(true, archivedGroups);
    }

    private void awardAttendeeBadges(List<User> members) {
        for (User member : members) {
            awardBadge(member, BadgeType.EVENT_ATTENDEE);
        }
    }

    private void awardBadge(User user, BadgeType badgeType) {
        if (user == null || user.getId() == null) {
            return;
        }

        if (userBadgeRepository.existsByUserIdAndBadgeType(user.getId(), badgeType)) {
            return;
        }

        UserBadge badge = new UserBadge();
        badge.setUser(user);
        badge.setBadgeType(badgeType);

        try {
            userBadgeRepository.save(badge);
        } catch (DataIntegrityViolationException ignored) {
            // Another job runner may have inserted the same user badge first.
        }
    }
}
