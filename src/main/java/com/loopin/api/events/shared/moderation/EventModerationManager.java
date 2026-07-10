package com.loopin.api.events.shared.moderation;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.shared.policy.EventLifecyclePolicy;
import com.loopin.api.moderation.ContentModerationService;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventModerationManager {

    private final ContentModerationService contentModerationService;

    public void apply(Event event, String title, String description) {
        if (!contentModerationService.moderate(title, description).isApproved()) {
            event.setModerationStatus(ContentModerationStatus.PENDING_REVIEW);
            event.setModerationRejectionReason(null);
            EventLifecyclePolicy.markPendingModeration(event);
            return;
        }

        event.setModerationStatus(ContentModerationStatus.APPROVED);
        event.setModerationRejectionReason(null);
    }
}
