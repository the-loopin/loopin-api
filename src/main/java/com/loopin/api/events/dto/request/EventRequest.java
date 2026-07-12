package com.loopin.api.events.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Fields shared by event create and update requests. */
public interface EventRequest {

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    LocalDateTime getStartDateTime();

    LocalDateTime getEndDateTime();

    Boolean getIsFree();

    BigDecimal getPrice();

    String getImageUrl();

    List<UUID> getInterestIds();
}
