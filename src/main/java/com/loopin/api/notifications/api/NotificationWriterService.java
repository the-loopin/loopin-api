package com.loopin.api.notifications.api;

import com.loopin.api.notifications.service.NotificationCommand;
import com.loopin.api.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Collection;

@Component
@RequiredArgsConstructor
class NotificationWriterService implements NotificationWriter {
    private final NotificationService notificationService;
    public void write(NotificationCommand command) { notificationService.create(command); }
    public void writeAll(Collection<NotificationCommand> commands) { notificationService.createAll(commands); }
}
