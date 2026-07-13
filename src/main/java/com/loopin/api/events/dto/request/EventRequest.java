package com.loopin.api.events.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Defines the fields shared by event create and update requests.
 */
public interface EventRequest {

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    LocalDateTime getStartDateTime();

    LocalDateTime getEndDateTime();

    Boolean getIsFree();

    BigDecimal getPrice();

    UUID getImageMediaId();

    List<UUID> getInterestIds();
}
