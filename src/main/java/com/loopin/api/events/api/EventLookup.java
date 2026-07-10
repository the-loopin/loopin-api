package com.loopin.api.events.api;

import com.loopin.api.events.entity.Event;
import java.util.UUID;

public interface EventLookup {
    Event findActiveByPublicId(UUID publicId);
}
