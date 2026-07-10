package com.loopin.api.events.delete;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.events.repository.EventGroupRepository;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.access.EventAccessPolicy;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.notification.EventMemberNotifier;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteEventHandlerTest {

    @Test
    void handle_softDeletesEventRemovesInterestsAndArchivesActiveGroups() {
        EventRepository repository = mock(EventRepository.class);
        EventInterestRepository interestRepository = mock(EventInterestRepository.class);
        EventGroupRepository groupRepository = mock(EventGroupRepository.class);
        EventFinder finder = mock(EventFinder.class);
        EventAccessPolicy accessPolicy = mock(EventAccessPolicy.class);
        EventMemberNotifier memberNotifier = mock(EventMemberNotifier.class);
        DeleteEventHandler handler = new DeleteEventHandler(repository, interestRepository, groupRepository,
                finder, accessPolicy, memberNotifier);
        UUID id = UUID.randomUUID();
        Event event = new Event();
        event.setId(7L);
        event.setPublicId(id);
        event.setTitle("Event");
        User user = new User("owner@loopin.test", "Owner", null);
        EventGroup openGroup = new EventGroup();
        openGroup.setStatus(GroupStatus.OPEN);
        EventGroup fullGroup = new EventGroup();
        fullGroup.setStatus(GroupStatus.FULL);
        when(finder.findCurrentUser("owner@loopin.test")).thenReturn(user);
        when(finder.findActiveEventById(id)).thenReturn(event);
        when(groupRepository.findByEventIdAndStatusNot(7L, GroupStatus.ARCHIVED))
                .thenReturn(List.of(openGroup, fullGroup));

        handler.handle(new DeleteEventCommand(id, "owner@loopin.test"));

        assertTrue(event.isDeleted());
        verify(accessPolicy).requireOwnerOrAdmin(event, user);
        verify(memberNotifier).notifyMembers(event, "Event deleted", "\"Event\" has been deleted.");
        verify(interestRepository).deleteByEvent_Id(7L);
        verify(groupRepository).save(openGroup);
        verify(groupRepository).save(fullGroup);
        verify(repository).save(event);
    }
}
