package com.loopin.api.events.shared.specification;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.listpublishedevents.ListPublishedEventsQuery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Specifications shared by public event discovery read paths. */
@Component
public class PublishedEventSpecifications {

    public Specification<Event> forPublishedListing(ListPublishedEventsQuery query) {
        return Specification.where(notDeleted())
                .and(hasStatus(EventStatus.PUBLISHED))
                .and(hasType(query.type()))
                .and(hasCategory(query.category()))
                .and(cityContains(query.city()))
                .and(hasIsFree(query.isFree()))
                .and(searchInTitleOrDescription(query.search()))
                .and(startsOnOrAfter(query.startDate()))
                .and(startsOnOrBefore(query.endDate()));
    }

    public Specification<Event> activePublishedAt(LocalDateTime now) {
        return Specification.where(notDeleted())
                .and(hasStatus(EventStatus.PUBLISHED))
                .and((root, query, criteriaBuilder) ->
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endDateTime"), now));
    }

    private Specification<Event> alwaysTrue() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    private Specification<Event> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    private Specification<Event> hasStatus(EventStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<Event> hasType(EventType type) {
        if (type == null) {
            return alwaysTrue();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
    }

    private Specification<Event> hasCategory(EventCategory category) {
        if (category == null) {
            return alwaysTrue();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category"), category);
    }

    private Specification<Event> cityContains(String city) {
        if (city == null || city.isBlank()) {
            return alwaysTrue();
        }
        String cityPattern = "%" + city.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), cityPattern);
    }

    private Specification<Event> hasIsFree(Boolean isFree) {
        if (isFree == null) {
            return alwaysTrue();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isFree"), isFree);
    }

    private Specification<Event> searchInTitleOrDescription(String search) {
        if (search == null || search.isBlank()) {
            return alwaysTrue();
        }
        String searchPattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern)
        );
    }

    private Specification<Event> startsOnOrAfter(LocalDate startDate) {
        if (startDate == null) {
            return alwaysTrue();
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("startDateTime"), startDate.atStartOfDay());
    }

    private Specification<Event> startsOnOrBefore(LocalDate endDate) {
        if (endDate == null) {
            return alwaysTrue();
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(root.get("startDateTime"), endDate.plusDays(1).atStartOfDay());
    }
}
