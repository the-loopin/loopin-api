package com.loopin.api.dto.event.response;

import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private EventType type;
    private EventCategory category;
    private String city;
    private String address;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Boolean isFree;
    private BigDecimal price;
    private String organizerName;
    private String imageUrl;
    private EventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}