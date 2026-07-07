package com.loopin.api.mapper;

import com.loopin.api.common.enums.EventCategory;
import com.loopin.api.common.enums.EventStatus;
import com.loopin.api.common.enums.EventType;
import com.loopin.api.dto.event.request.EventCreateRequest;
import com.loopin.api.dto.event.request.EventUpdateRequest;
import com.loopin.api.dto.event.response.EventResponse;
import com.loopin.api.entity.Event;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventMapperTest {

    private EventMapper eventMapper;

    @BeforeEach
    void setUp() {
        eventMapper = Mappers.getMapper(EventMapper.class);
    }

    @Test
    void toEntity_ValidRequest_MapsAllFields() {
        EventCreateRequest request = new EventCreateRequest();
        request.setTitle("Test Title");
        request.setDescription("Test Desc");
        request.setType(EventType.EVENT);
        request.setCategory(EventCategory.TECH);
        request.setCity("Test City");
        request.setAddress("Test Address");
        request.setStartDateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        request.setEndDateTime(LocalDateTime.of(2025, 1, 1, 12, 0));
        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);
        request.setOrganizerName("Test Org");
        request.setImageUrl("http://image.com");
        request.setStatus(EventStatus.DRAFT);

        Event event = eventMapper.toEntity(request);

        assertEquals("Test Title", event.getTitle());
        assertEquals("Test Desc", event.getDescription());
        assertEquals(EventType.EVENT, event.getType());
        assertEquals(EventCategory.TECH, event.getCategory());
        assertEquals("Test City", event.getCity());
        assertEquals("Test Address", event.getAddress());
        assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0), event.getStartDateTime());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), event.getEndDateTime());
        assertTrue(event.getIsFree());
        assertEquals(BigDecimal.ZERO, event.getPrice());
        assertEquals("Test Org", event.getOrganizerName());
        assertEquals("http://image.com", event.getImageUrl());
        assertEquals(EventStatus.DRAFT, event.getStatus());
    }

    @Test
    void toEntity_NullStatus_DefaultsToPublished() {
        EventCreateRequest request = new EventCreateRequest();
        request.setStatus(null);

        Event event = eventMapper.toEntity(request);

        assertEquals(EventStatus.PUBLISHED, event.getStatus());
    }

    @Test
    void updateEntity_ValidRequest_UpdatesAllFields() {
        Event event = new Event();
        EventUpdateRequest request = new EventUpdateRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Desc");
        request.setType(EventType.EVENT);
        request.setCategory(EventCategory.TECH);
        request.setCity("Updated City");
        request.setAddress("Updated Address");
        request.setStartDateTime(LocalDateTime.of(2025, 2, 1, 10, 0));
        request.setEndDateTime(LocalDateTime.of(2025, 2, 1, 12, 0));
        request.setIsFree(false);
        request.setPrice(BigDecimal.TEN);
        request.setOrganizerName("Updated Org");
        request.setImageUrl("http://updated.com");
        request.setStatus(EventStatus.CANCELLED);

        eventMapper.updateEntity(event, request);

        assertEquals("Updated Title", event.getTitle());
        assertEquals("Updated Desc", event.getDescription());
        assertEquals(EventType.EVENT, event.getType());
        assertEquals(EventCategory.TECH, event.getCategory());
        assertEquals("Updated City", event.getCity());
        assertEquals("Updated Address", event.getAddress());
        assertEquals(LocalDateTime.of(2025, 2, 1, 10, 0), event.getStartDateTime());
        assertEquals(LocalDateTime.of(2025, 2, 1, 12, 0), event.getEndDateTime());
        assertEquals(false, event.getIsFree());
        assertEquals(BigDecimal.TEN, event.getPrice());
        assertEquals("Updated Org", event.getOrganizerName());
        assertEquals("http://updated.com", event.getImageUrl());
        assertEquals(EventStatus.CANCELLED, event.getStatus());
    }

    @Test
    void toResponse_ValidEvent_MapsAllFieldsAndUsesPublicId() {
        Event event = new Event();
        event.setId(999L); // Internal ID that MUST NOT be mapped to output ID
        UUID publicId = UUID.randomUUID();
        event.setPublicId(publicId);
        event.setTitle("Test Title");
        event.setDescription("Test Desc");
        event.setType(EventType.EVENT);
        event.setCategory(EventCategory.TECH);
        event.setCity("Test City");
        event.setAddress("Test Address");
        event.setStartDateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        event.setEndDateTime(LocalDateTime.of(2025, 1, 1, 12, 0));
        event.setIsFree(true);
        event.setPrice(BigDecimal.ZERO);
        event.setOrganizerName("Test Org");
        event.setImageUrl("http://image.com");
        event.setStatus(EventStatus.PUBLISHED);
        
        LocalDateTime now = LocalDateTime.now();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        EventResponse response = eventMapper.toResponse(event);

        assertEquals(publicId, response.getId()); // EXPLICIT CHECK: Uses publicId, not 999L
        assertEquals("Test Title", response.getTitle());
        assertEquals("Test Desc", response.getDescription());
        assertEquals(EventType.EVENT, response.getType());
        assertEquals(EventCategory.TECH, response.getCategory());
        assertEquals("Test City", response.getCity());
        assertEquals("Test Address", response.getAddress());
        assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0), response.getStartDateTime());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), response.getEndDateTime());
        assertTrue(response.getIsFree());
        assertEquals(BigDecimal.ZERO, response.getPrice());
        assertEquals("Test Org", response.getOrganizerName());
        assertEquals("http://image.com", response.getImageUrl());
        assertEquals(EventStatus.PUBLISHED, response.getStatus());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }
}
