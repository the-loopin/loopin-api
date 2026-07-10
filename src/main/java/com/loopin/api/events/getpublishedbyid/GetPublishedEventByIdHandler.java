package com.loopin.api.events.getpublishedbyid;

import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.mapper.EventMapper;
import com.loopin.api.events.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class GetPublishedEventByIdHandler {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Cacheable(value = "eventById", key = "#query.id")
    @Transactional(readOnly = true)
    public EventResponse handle(GetPublishedEventByIdQuery query) {
        Event event = eventRepository.findPublishedByPublicIdWithInterests(query.id())
                .orElseThrow(() -> new NoSuchElementException("Published event not found with id: " + query.id()));

        return eventMapper.toResponse(event);
    }
}
