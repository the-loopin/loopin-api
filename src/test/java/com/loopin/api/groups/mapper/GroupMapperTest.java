package com.loopin.api.groups.mapper;

import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.users.entity.User;
import com.loopin.api.groups.repository.GroupMemberRepository;
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
    private GroupMapper groupMapper;

    @BeforeEach
    void setUp() {
        groupMemberRepository = mock(GroupMemberRepository.class);
        groupMapper = new GroupMapper(groupMemberRepository);
    }

    @Test
    void toEntity_ValidRequest_MapsFieldsAndSetsOpenStatus() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setTitle("Test Group");
        request.setGroupSize(com.loopin.api.groups.enums.GroupSizeType.FOUR);
        request.setMaxMembers(20);
        request.setGroupNote("Test Note");

        User admin = new User();
        Event event = new Event();

        EventGroup group = groupMapper.toEntity(request, admin, event);

        assertEquals("Test Group", group.getTitle());
        assertEquals(com.loopin.api.groups.enums.GroupSizeType.FOUR, group.getGroupSize());
        assertEquals(20, group.getMaxMembers());
        assertEquals("Test Note", group.getGroupNote());
        assertEquals(admin, group.getAdmin());
        assertEquals(event, group.getEvent());
        assertEquals(GroupStatus.OPEN, group.getStatus());
    }

    @Test
    void toGroupResponse_ValidGroupWithEvent_MapsFieldsAndUsesPublicId() {
        EventGroup group = new EventGroup();
        group.setId(555L); // Internal ID MUST NOT be exposed
        UUID groupPublicId = UUID.randomUUID();
        group.setPublicId(groupPublicId);
        group.setTitle("Test Group");
        group.setGroupSize(com.loopin.api.groups.enums.GroupSizeType.FOUR);
        group.setMaxMembers(20);
        group.setGroupNote("Test Note");
        group.setStatus(GroupStatus.OPEN);
        LocalDateTime now = LocalDateTime.now();
        group.setCreatedAt(now);

        Event event = new Event();
        UUID eventPublicId = UUID.randomUUID();
        event.setPublicId(eventPublicId);
        group.setEvent(event);

        User admin = new User();
        UUID adminPublicId = UUID.randomUUID();
        admin.setPublicId(adminPublicId);
        admin.setEmail("admin@test.com");
        group.setAdmin(admin);

        when(groupMemberRepository.countByGroupId(555L)).thenReturn(5);

        GroupResponse response = groupMapper.toGroupResponse(group);

        assertEquals(groupPublicId, response.getId()); // EXPLICIT CHECK: Uses publicId
        assertEquals(eventPublicId, response.getEventId()); // EXPLICIT CHECK: Uses publicId
        assertEquals(adminPublicId, response.getAdminId()); // EXPLICIT CHECK: Uses publicId
        assertEquals("admin@test.com", response.getAdminUsername());
        assertEquals("Test Group", response.getTitle());
        assertEquals(com.loopin.api.groups.enums.GroupSizeType.FOUR, response.getGroupSize());
        assertEquals(20, response.getMaxMembers());
        assertEquals(GroupStatus.OPEN, response.getStatus());
        assertEquals("Test Note", response.getGroupNote());
        assertEquals(5, response.getMemberCount());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    void toGroupResponse_GroupWithoutEvent_MapsFieldsWithNullEventId() {
        EventGroup group = new EventGroup();
        group.setId(555L);
        UUID groupPublicId = UUID.randomUUID();
        group.setPublicId(groupPublicId);
        group.setEvent(null); // Explicit null event

        User admin = new User();
        UUID adminPublicId = UUID.randomUUID();
        admin.setPublicId(adminPublicId);
        group.setAdmin(admin);

        when(groupMemberRepository.countByGroupId(555L)).thenReturn(0);

        GroupResponse response = groupMapper.toGroupResponse(group);

        assertEquals(groupPublicId, response.getId());
        assertNull(response.getEventId());
        assertEquals(adminPublicId, response.getAdminId());
    }
}
