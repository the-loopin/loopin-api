package com.loopin.api.recommendation.event;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventInterest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
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
                        interestText(event),
                        Boolean.TRUE.equals(event.getIsFree()) ? "free event" : "paid event"
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String interestText(Event event) {
        if (event.getInterests() == null || event.getInterests().isEmpty()) {
            return null;
        }

        return event.getInterests()
                .stream()
                .map(EventInterest::getInterest)
                .filter(interest -> interest != null && interest.getName() != null)
                .sorted(Comparator.comparing(interest -> interest.getName().toLowerCase()))
                .map(interest -> "interest: " + interest.getName())
                .collect(Collectors.joining("\n"));
    }
}
