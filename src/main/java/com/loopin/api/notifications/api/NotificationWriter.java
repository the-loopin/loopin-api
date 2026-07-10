package com.loopin.api.notifications.api;

import com.loopin.api.notifications.service.NotificationCommand;
import java.util.Collection;

public interface NotificationWriter {
    void write(NotificationCommand command);
    void writeAll(Collection<NotificationCommand> commands);
}
