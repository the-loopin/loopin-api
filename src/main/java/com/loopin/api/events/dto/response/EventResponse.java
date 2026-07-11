package com.loopin.api.events.dto.response;

import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.interests.dto.InterestResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private UUID id;
    private String title;
    private String description;
    private EventType type;
    private EventCategory category;
    private String city;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Boolean isFree;
    private BigDecimal price;
    private String organizerName;
    private String imageUrl;
    private EventStatus status;
    private ContentModerationStatus moderationStatus;
    private String moderationRejectionReason;
    private List<InterestResponse> interests;
    private Long loopedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Backwards-compatible constructor for internal callers that predate
     * explicit moderation fields. Existing events are treated as approved.
     */
    public EventResponse(
            UUID id,
            String title,
            String description,
            EventType type,
            EventCategory category,
            String city,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Boolean isFree,
            BigDecimal price,
            String organizerName,
            String imageUrl,
            EventStatus status,
            List<InterestResponse> interests,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(
                id, title, description, type, category, city, address, latitude, longitude,
                startDateTime, endDateTime, isFree, price, organizerName, imageUrl, status,
                ContentModerationStatus.APPROVED, null, interests, 0L, createdAt, updatedAt
        );
    }
}
