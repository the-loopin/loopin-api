package com.loopin.api.events.shared.notification;

import com.loopin.api.events.entity.Event;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventMemberNotifier {

    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;

    public void notifyMembers(Event event, String title, String message) {
        notificationService.createAll(groupMemberRepository.findDistinctActiveUsersByEventId(event.getId())
                .stream()
                .map(recipient -> new NotificationCommand(
                        recipient,
                        NotificationType.EVENT_UPDATE,
                        title,
                        message,
                        NotificationReferenceType.EVENT,
                        event.getPublicId()))
                .toList());
    }
}
