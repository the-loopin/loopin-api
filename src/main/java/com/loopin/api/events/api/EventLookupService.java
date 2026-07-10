package com.loopin.api.events.api;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class EventLookupService implements EventLookup {
    private final EventRepository eventRepository;
    public Event findActiveByPublicId(UUID publicId) {
        return eventRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + publicId));
    }
}
