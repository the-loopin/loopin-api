package com.loopin.api.events.mapper;

import com.loopin.api.events.dto.request.EventCreateRequest;
import com.loopin.api.events.dto.request.EventUpdateRequest;
import com.loopin.api.events.dto.response.EventResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.interests.mapper.InterestMapper;
import com.loopin.api.media.dto.response.MediaReferenceResponse;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.mapper.MediaReferenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventMapperTest {

    @Mock
    private InterestMapper interestMapper;

    @Mock
    private MediaReferenceMapper mediaReferenceMapper;

    @InjectMocks
    private EventMapperImpl eventMapper;

    @Test
    void toEntity_validRequest_mapsDomainFieldsButNotMedia() {
        EventCreateRequest request =
            validCreateRequest();

        request.setImageMediaId(
            UUID.randomUUID()
        );

        Event event =
            eventMapper.toEntity(request);

        assertEquals(
            "Test Title",
            event.getTitle()
        );

        assertEquals(
            "Test Desc",
            event.getDescription()
        );

        assertEquals(
            EventType.EVENT,
            event.getType()
        );

        assertEquals(
            EventCategory.TECH,
            event.getCategory()
        );

        assertEquals(
            "Test City",
            event.getCity()
        );

        assertEquals(
            "Test Address",
            event.getAddress()
        );

        assertEquals(
            new BigDecimal("40.376200"),
            event.getLatitude()
        );

        assertEquals(
            new BigDecimal("49.844700"),
            event.getLongitude()
        );

        assertEquals(
            LocalDateTime.of(
                2030,
                1,
                1,
                10,
                0
            ),
            event.getStartDateTime()
        );

        assertEquals(
            LocalDateTime.of(
                2030,
                1,
                1,
                12,
                0
            ),
            event.getEndDateTime()
        );

        assertTrue(event.getIsFree());

        assertEquals(
            BigDecimal.ZERO,
            event.getPrice()
        );

        assertEquals(
            "Test Org",
            event.getOrganizerName()
        );

        /*
         * Media must be attached by the command handler,
         * not by MapStruct.
         */
        assertNull(event.getImageMedia());

        assertEquals(
            EventStatus.PUBLISHED,
            event.getStatus()
        );
    }

    @Test
    void updateEntity_preservesImageMediaForHandler() {
        Event event = new Event();

        MediaAsset currentImage =
            mock(MediaAsset.class);

        event.setImageMedia(currentImage);
        event.setStatus(EventStatus.PUBLISHED);

        EventUpdateRequest request =
            validUpdateRequest();

        request.setImageMediaId(
            UUID.randomUUID()
        );

        eventMapper.updateEntity(
            event,
            request
        );

        assertEquals(
            "Updated Title",
            event.getTitle()
        );

        assertEquals(
            "Updated Desc",
            event.getDescription()
        );

        /*
         * The handler replaces this field after media validation.
         */
        assertSame(
            currentImage,
            event.getImageMedia()
        );

        assertEquals(
            EventStatus.PUBLISHED,
            event.getStatus()
        );
    }

    @Test
    void toResponse_mapsMediaReference() {
        Event event = new Event();

        UUID eventId = UUID.randomUUID();

        event.setPublicId(eventId);
        event.setTitle("Test Title");
        event.setDescription("Test Desc");
        event.setType(EventType.EVENT);
        event.setCategory(EventCategory.TECH);
        event.setCity("Baku");
        event.setStartDateTime(
            LocalDateTime.of(
                2030,
                1,
                1,
                10,
                0
            )
        );
        event.setEndDateTime(
            LocalDateTime.of(
                2030,
                1,
                1,
                12,
                0
            )
        );
        event.setIsFree(true);
        event.setPrice(BigDecimal.ZERO);
        event.setOrganizerName("Loopin");
        event.setStatus(EventStatus.PUBLISHED);

        MediaAsset imageMedia =
            mock(MediaAsset.class);

        event.setImageMedia(imageMedia);

        MediaReferenceResponse imageResponse =
            new MediaReferenceResponse(
                UUID.randomUUID(),
                "image/webp",
                250_000L
            );

        when(
            mediaReferenceMapper.toResponse(
                imageMedia
            )
        ).thenReturn(imageResponse);

        EventResponse response =
            eventMapper.toResponse(event);

        assertEquals(
            eventId,
            response.getId()
        );

        assertEquals(
            imageResponse,
            response.getImage()
        );

        /*
         * The legacy URL field remains null until the delivery
         * URL strategy is introduced.
         */
        assertNull(response.getImageUrl());
    }

    private EventCreateRequest validCreateRequest() {
        EventCreateRequest request =
            new EventCreateRequest();

        request.setTitle("Test Title");
        request.setDescription("Test Desc");
        request.setType(EventType.EVENT);
        request.setCategory(EventCategory.TECH);
        request.setCity("Test City");
        request.setAddress("Test Address");

        request.setLatitude(
            new BigDecimal("40.376200")
        );

        request.setLongitude(
            new BigDecimal("49.844700")
        );

        request.setStartDateTime(
            LocalDateTime.of(
                2030,
                1,
                1,
                10,
                0
            )
        );

        request.setEndDateTime(
            LocalDateTime.of(
                2030,
                1,
                1,
                12,
                0
            )
        );

        request.setIsFree(true);
        request.setPrice(BigDecimal.ZERO);
        request.setOrganizerName("Test Org");

        return request;
    }

    private EventUpdateRequest validUpdateRequest() {
        EventUpdateRequest request =
            new EventUpdateRequest();

        request.setTitle("Updated Title");
        request.setDescription("Updated Desc");
        request.setType(EventType.EVENT);
        request.setCategory(EventCategory.TECH);
        request.setCity("Updated City");
        request.setAddress("Updated Address");

        request.setLatitude(
            new BigDecimal("40.409300")
        );

        request.setLongitude(
            new BigDecimal("49.867100")
        );

        request.setStartDateTime(
            LocalDateTime.of(
                2030,
                2,
                1,
                10,
                0
            )
        );

        request.setEndDateTime(
            LocalDateTime.of(
                2030,
                2,
                1,
                12,
                0
            )
        );

        request.setIsFree(false);
        request.setPrice(BigDecimal.TEN);
        request.setOrganizerName("Updated Org");

        return request;
    }
}
