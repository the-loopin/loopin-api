package com.loopin.api.job;

import com.loopin.api.common.enums.BadgeType;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.GroupStatus;
import com.loopin.api.entity.Event;
import com.loopin.api.entity.EventGroup;
import com.loopin.api.entity.GroupMember;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserBadge;
import com.loopin.api.repository.EventGroupRepository;
import com.loopin.api.repository.EventRepository;
import com.loopin.api.repository.GroupMemberRepository;
import com.loopin.api.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventCompletionProcessor {

    private final EventRepository eventRepository;
    private final EventGroupRepository eventGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserBadgeRepository userBadgeRepository;

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
        List<EventGroup> groups = eventGroupRepository.findByEventIdAndStatusNot(eventId, GroupStatus.ARCHIVED);
        for (EventGroup group : groups) {
            group.setStatus(GroupStatus.ARCHIVED);
            eventGroupRepository.save(group);
            archivedGroups++;

            awardBadge(group.getAdmin(), BadgeType.GROUP_CREATOR);
            awardAttendeeBadges(group);
        }

        return new EventCompletionResult(true, archivedGroups);
    }

    private void awardAttendeeBadges(EventGroup group) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());

        for (GroupMember member : members) {
            awardBadge(member.getUser(), BadgeType.EVENT_ATTENDEE);
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
