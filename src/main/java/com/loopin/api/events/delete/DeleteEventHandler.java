package com.loopin.api.events.delete;

import com.loopin.api.events.entity.Event;
import com.loopin.api.groups.api.GroupLifecycle;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.access.EventAccessPolicy;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.notification.EventMemberNotifier;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeleteEventHandler {

    private final EventRepository eventRepository;
    private final EventInterestRepository eventInterestRepository;
    private final GroupLifecycle groupLifecycle;
    private final EventFinder eventFinder;
    private final EventAccessPolicy eventAccessPolicy;
    private final EventMemberNotifier eventMemberNotifier;

    @Caching(evict = {
        @CacheEvict(value = "publishedEvents", allEntries = true),
        @CacheEvict(value = "eventById", key = "#command.id")
    })
    @Transactional
    public void handle(DeleteEventCommand command) {
        User currentUser = eventFinder.findCurrentUser(command.currentUsername());
        Event event = eventFinder.findActiveEventById(command.id());
        eventAccessPolicy.requireOwnerOrAdmin(event, currentUser);
        eventMemberNotifier.notifyMembers(
                event,
                "Event deleted",
                "\"" + event.getTitle() + "\" has been deleted.");
        eventInterestRepository.deleteByEvent_Id(event.getId());
        groupLifecycle.archiveActiveGroupsForEvent(event.getId());
        event.markAsDeleted();
        eventRepository.save(event);
    }

}
