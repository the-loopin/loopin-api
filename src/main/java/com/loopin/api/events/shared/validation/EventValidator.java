package com.loopin.api.events.shared.validation;

import com.loopin.api.common.exception.DuplicateResourceException;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EventValidator {

    private final EventRepository eventRepository;

    public void validateNoDuplicate(String title, String city, LocalDateTime startDateTime) {
        boolean exists = eventRepository.exists(Specification.where(notDeleted())
                .and(titleEqualsIgnoreCase(title))
                .and(cityEqualsIgnoreCase(city))
                .and(startsAt(startDateTime)));

        if (exists) {
            throw new DuplicateResourceException("Event already exists with same title, city, and start date time");
        }
    }

    private Specification<Event> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    private Specification<Event> titleEqualsIgnoreCase(String title) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("title")), title.toLowerCase());
    }

    private Specification<Event> cityEqualsIgnoreCase(String city) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("city")), city.toLowerCase());
    }

    private Specification<Event> startsAt(LocalDateTime startDateTime) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("startDateTime"), startDateTime);
    }
}
