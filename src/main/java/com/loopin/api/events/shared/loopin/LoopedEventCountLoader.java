package com.loopin.api.events.shared.loopin;

import com.loopin.api.events.repository.UserLoopedEventRepository;
import com.loopin.api.events.repository.projection.LoopedEventCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LoopedEventCountLoader {

    private final UserLoopedEventRepository repository;

    public Map<Long, Long> load(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        return repository.countByEventIds(eventIds)
            .stream()
            .collect(Collectors.toMap(
                LoopedEventCountProjection::getEventId,
                LoopedEventCountProjection::getLoopedCount
            ));
    }
}
