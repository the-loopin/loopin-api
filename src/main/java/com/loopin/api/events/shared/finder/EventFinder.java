package com.loopin.api.events.shared.finder;

import com.loopin.api.common.exception.ResourceNotFoundException;
import com.loopin.api.common.exception.UnauthorizedException;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.api.UserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventFinder {

    private final EventRepository eventRepository;
    private final UserLookup userLookup;

    public User findCurrentUser(String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }
        return userLookup.findByEmail(currentUsername);
    }

    public Event findActiveEventById(UUID id) {
        return eventRepository.findOne(Specification.where(notDeleted()).and(hasPublicId(id)))
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + id));
    }

    private Specification<Event> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    private Specification<Event> hasPublicId(UUID id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("publicId"), id);
    }
}
