package com.loopin.api.events.shared.validation;

import com.loopin.api.events.dto.request.EventRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class EventRequestValidator {

    static final Duration MINIMUM_LEAD_TIME = Duration.ofMinutes(30);
    static final Duration MAXIMUM_DURATION = Duration.ofHours(24);
    static final int MAXIMUM_INTEREST_IDS = 10;
    static final int PRICE_SCALE = 2;
    static final BigDecimal MINIMUM_PAID_PRICE = new BigDecimal("0.01");
    static final BigDecimal MAXIMUM_PRICE = new BigDecimal("1000000.00");

    private final Clock clock;

    public EventRequestValidator(Clock clock) {
        this.clock = clock;
    }

    public void validate(EventRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateDates(request, errors);
        validatePrice(request, errors);
        validateImageUrl(request.getImageUrl(), errors);
        validateCoordinates(request, errors);
        validateInterestIds(request.getInterestIds(), errors);

        if (!errors.isEmpty()) {
            throw new EventRequestValidationException(errors);
        }
    }

    private void validateDates(EventRequest request, Map<String, String> errors) {
        LocalDateTime start = request.getStartDateTime();
        LocalDateTime end = request.getEndDateTime();
        if (start == null || end == null) {
            return;
        }

        if (start.isBefore(LocalDateTime.now(clock).plus(MINIMUM_LEAD_TIME))) {
            errors.put("startDateTime", "Start date and time must be at least 30 minutes in the future");
        }
        if (!end.isAfter(start)) {
            errors.put("endDateTime", "End date and time must be after start date and time");
        } else if (Duration.between(start, end).compareTo(MAXIMUM_DURATION) > 0) {
            errors.put("endDateTime", "Event duration must not exceed 24 hours");
        }
    }

    private void validatePrice(EventRequest request, Map<String, String> errors) {
        Boolean isFree = request.getIsFree();
        BigDecimal price = request.getPrice();
        if (isFree == null) {
            return;
        }
        if (isFree) {
            if (price != null && price.compareTo(BigDecimal.ZERO) != 0) {
                errors.put("price", "Free events must have price 0 or null");
            }
            return;
        }

        if (price == null || price.compareTo(MINIMUM_PAID_PRICE) < 0) {
            errors.put("price", "Paid events must have a price of at least 0.01");
        } else if (price.scale() > PRICE_SCALE) {
            errors.put("price", "Price must have at most 2 decimal places");
        } else if (price.compareTo(MAXIMUM_PRICE) > 0) {
            errors.put("price", "Price must not exceed 1000000.00");
        }
    }

    private void validateImageUrl(String imageUrl, Map<String, String> errors) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        try {
            URI uri = new URI(imageUrl);
            if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                errors.put("imageUrl", "Image URL must use HTTP or HTTPS");
            }
        } catch (URISyntaxException exception) {
            errors.put("imageUrl", "Image URL must use HTTP or HTTPS");
        }
    }

    private void validateCoordinates(EventRequest request, Map<String, String> errors) {
        if ((request.getLatitude() == null) != (request.getLongitude() == null)) {
            errors.put("coordinates", "Latitude and longitude must be provided together");
        }
    }

    private void validateInterestIds(List<UUID> interestIds, Map<String, String> errors) {
        if (interestIds == null) {
            return;
        }
        if (interestIds.size() > MAXIMUM_INTEREST_IDS) {
            errors.put("interestIds", "At most 10 interests may be selected");
            return;
        }
        if (interestIds.stream().anyMatch(java.util.Objects::isNull)) {
            errors.put("interestIds", "Interest IDs must not contain null values");
            return;
        }
        if (interestIds.stream().distinct().count() != interestIds.size()) {
            errors.put("interestIds", "Duplicate interests are not allowed");
        }
    }
}
