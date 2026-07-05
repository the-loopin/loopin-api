package com.loopin.api.recommendation;

import com.loopin.api.entity.Event;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class EventEmbeddingTextBuilder {

    public String build(Event event) {
        return Stream.of(
                        event.getTitle(),
                        event.getDescription(),
                        event.getType() == null ? null : event.getType().name(),
                        event.getCategory() == null ? null : event.getCategory().name(),
                        event.getCity(),
                        event.getAddress(),
                        event.getOrganizerName(),
                        Boolean.TRUE.equals(event.getIsFree()) ? "free event" : "paid event"
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n"));
    }
}
