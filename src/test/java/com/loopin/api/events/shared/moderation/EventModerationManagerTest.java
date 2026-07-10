package com.loopin.api.events.shared.moderation;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.moderation.ContentModerationProperties;
import com.loopin.api.moderation.ContentModerationService;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventModerationManagerTest {

    @Test
    void apply_BlockedContent_MarksEventPendingAndNotPublic() {
        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setBannedWords(List.of("scam"));
        EventModerationManager manager = new EventModerationManager(new ContentModerationService(properties));
        Event event = new Event();
        event.setStatus(EventStatus.PUBLISHED);

        manager.apply(event, "Not a scam", "Join us");

        assertEquals(ContentModerationStatus.PENDING_REVIEW, event.getModerationStatus());
        assertEquals(EventStatus.DRAFT, event.getStatus());
    }
}
