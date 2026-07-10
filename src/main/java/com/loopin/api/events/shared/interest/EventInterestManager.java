package com.loopin.api.events.shared.interest;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventInterest;
import com.loopin.api.interests.entity.Interest;
import com.loopin.api.interests.repository.InterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventInterestManager {

    private final InterestRepository interestRepository;

    public void replace(Event event, List<UUID> interestIds) {
        List<UUID> requestedInterestIds = interestIds == null ? List.of() : interestIds;
        Set<UUID> uniqueInterestIds = new LinkedHashSet<>(requestedInterestIds);
        if (uniqueInterestIds.size() != requestedInterestIds.size()) {
            throw new IllegalArgumentException("Duplicate interests are not allowed.");
        }

        Map<UUID, Interest> interestsByPublicId = findByPublicIds(uniqueInterestIds);
        Set<EventInterest> newInterests = uniqueInterestIds.stream()
                .map(interestId -> new EventInterest(event, interestsByPublicId.get(interestId)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (event.getInterests() == null) {
            event.setInterests(new LinkedHashSet<>());
        }
        event.getInterests().clear();
        event.getInterests().addAll(newInterests);
    }

    private Map<UUID, Interest> findByPublicIds(Set<UUID> publicIds) {
        if (publicIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Interest> interestsByPublicId = interestRepository.findByPublicIdInAndDeletedAtIsNull(publicIds)
                .stream()
                .collect(Collectors.toMap(Interest::getPublicId, Function.identity()));
        if (interestsByPublicId.size() != publicIds.size()) {
            throw new NoSuchElementException("One or more interests were not found.");
        }
        return interestsByPublicId;
    }
}
