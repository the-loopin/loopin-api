package com.loopin.api.groups.mapper;

import com.loopin.api.events.entity.Event;
import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.media.dto.response.MediaReferenceResponse;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.mapper.MediaReferenceMapper;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupMapperTest {

    private GroupMemberRepository groupMemberRepository;
    private MediaReferenceMapper mediaReferenceMapper;

    private GroupMapper groupMapper;

    @BeforeEach
    void setUp() {
        groupMemberRepository =
            mock(GroupMemberRepository.class);

        mediaReferenceMapper =
            mock(MediaReferenceMapper.class);

        groupMapper = new GroupMapper(
            groupMemberRepository,
            mediaReferenceMapper
        );
    }

    @Test
    void toEntity_validRequest_mapsFieldsButDoesNotAttachMedia() {
        CreateGroupRequest request =
            new CreateGroupRequest();

        request.setTitle("Test Group");
        request.setGroupSize(
            GroupSizeType.FOUR
        );
        request.setMaxMembers(4);
        request.setGroupNote("Test Note");
        request.setImageMediaId(
            UUID.randomUUID()
        );

        User admin = new User();
        Event event = new Event();

        EventGroup group =
            groupMapper.toEntity(
                request,
                admin,
                event
            );

        assertEquals(
            "Test Group",
            group.getTitle()
        );

        assertEquals(
            GroupSizeType.FOUR,
            group.getGroupSize()
        );

        assertEquals(
            4,
            group.getMaxMembers()
        );

        assertEquals(
            "Test Note",
            group.getGroupNote()
        );

        assertEquals(
            admin,
            group.getAdmin()
        );

        assertEquals(
            event,
            group.getEvent()
        );

        assertEquals(
            GroupStatus.OPEN,
            group.getStatus()
        );

        /*
         * Media attachment is handled by CreateGroupHandler
         * after ownership and lifecycle validation.
         */
        assertNull(group.getImageMedia());
    }

    @Test
    void toGroupResponse_mapsImageAndPublicIdentifiers() {
        EventGroup group = new EventGroup();

        UUID groupPublicId = UUID.randomUUID();
        UUID eventPublicId = UUID.randomUUID();
        UUID adminPublicId = UUID.randomUUID();

        group.setId(555L);
        group.setPublicId(groupPublicId);
        group.setTitle("Test Group");
        group.setGroupSize(GroupSizeType.FOUR);
        group.setMaxMembers(4);
        group.setGroupNote("Test Note");
        group.setStatus(GroupStatus.OPEN);

        LocalDateTime createdAt =
            LocalDateTime.now();

        group.setCreatedAt(createdAt);

        Event event = new Event();
        event.setPublicId(eventPublicId);

        group.setEvent(event);

        User admin = new User();
        admin.setPublicId(adminPublicId);
        admin.setEmail("admin@test.com");

        group.setAdmin(admin);

        MediaAsset imageMedia =
            mock(MediaAsset.class);

        group.setImageMedia(imageMedia);

        MediaReferenceResponse imageResponse =
            new MediaReferenceResponse(
                UUID.randomUUID(),
                "image/webp",
                300_000L
            );

        when(
            mediaReferenceMapper.toResponse(
                imageMedia
            )
        ).thenReturn(imageResponse);

        when(
            groupMemberRepository.countByGroupId(
                555L
            )
        ).thenReturn(3);

        GroupResponse response =
            groupMapper.toGroupResponse(group);

        assertEquals(
            groupPublicId,
            response.getId()
        );

        assertEquals(
            eventPublicId,
            response.getEventId()
        );

        assertEquals(
            adminPublicId,
            response.getAdminId()
        );

        assertEquals(
            "admin@test.com",
            response.getAdminUsername()
        );

        assertEquals(
            imageResponse,
            response.getImage()
        );

        assertEquals(
            3,
            response.getMemberCount()
        );

        assertEquals(
            createdAt,
            response.getCreatedAt()
        );
    }

    @Test
    void toGroupResponse_groupWithoutEvent_mapsNullEventId() {
        EventGroup group = new EventGroup();

        group.setId(555L);
        group.setPublicId(UUID.randomUUID());
        group.setEvent(null);

        User admin = new User();

        admin.setPublicId(
            UUID.randomUUID()
        );

        group.setAdmin(admin);

        when(
            groupMemberRepository.countByGroupId(
                555L
            )
        ).thenReturn(0);

        GroupResponse response =
            groupMapper.toGroupResponse(group);

        assertNull(response.getEventId());
        assertNull(response.getImage());
    }
}
