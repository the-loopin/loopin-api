package com.loopin.api.events.loopinevent;

import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.dto.response.LoopedEventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.policy.EventLoopInPolicy;
import com.loopin.api.notifications.api.NotificationWriter;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoopInEventHandler {

    private final EventFinder eventFinder;
    private final EventLoopInPolicy eventLoopInPolicy;
    private final UserLoopedEventRepository loopedEventRepository;
    private final EventMapper eventMapper;
    private final NotificationWriter notificationWriter;

    @Transactional
    public LoopedEventResponse handle(LoopInEventCommand command) {
        User currentUser =
            eventFinder.findCurrentUser(command.currentUsername());

        Event event =
            eventFinder.findActiveEventById(command.eventId());

        eventLoopInPolicy.requireLoopable(event);

        int inserted = loopedEventRepository.insertIfAbsent(
            UUID.randomUUID(),
            LocalDateTime.now(),
            currentUser.getId(),
            event.getId()
        );

        boolean newlyCreated = inserted == 1;

        if (newlyCreated && shouldNotifyOwner(currentUser, event)) {
            notifyOwner(currentUser, event);
        }

        long count =
            loopedEventRepository.countByEventId(event.getId());

        EventResponse eventResponse = eventMapper.toResponse(event);

        return new LoopedEventResponse(eventResponse, count);
    }

    private boolean shouldNotifyOwner(User currentUser, Event event) {
        return event.getOwner() != null
            && !event.getOwner().getId().equals(currentUser.getId());
    }

    private void notifyOwner(User currentUser, Event event) {
        notificationWriter.write(new NotificationCommand(
            event.getOwner(),
            NotificationType.EVENT_LOOP_IN,
            "New Loop-in",
            currentUser.getName()
                + " looped into \"" + event.getTitle() + "\".",
            NotificationReferenceType.EVENT,
            event.getPublicId(),
            "event-loop-in:"
                + event.getPublicId()
                + ":"
                + currentUser.getPublicId()
        ));
    }
}
