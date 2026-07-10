package com.loopin.api.events.shared.notification;

import com.loopin.api.events.entity.Event;
import com.loopin.api.groups.api.GroupMemberLookup;
import com.loopin.api.notifications.enums.NotificationReferenceType;
import com.loopin.api.notifications.enums.NotificationType;
import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.notifications.api.NotificationWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventMemberNotifier {

    private final GroupMemberLookup groupMemberLookup;
    private final NotificationWriter notificationWriter;

    public void notifyMembers(Event event, String title, String message) {
        notificationWriter.writeAll(groupMemberLookup.findActiveUsersByEventId(event.getId())
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
