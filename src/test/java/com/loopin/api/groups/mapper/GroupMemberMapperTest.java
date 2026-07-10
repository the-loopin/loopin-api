package com.loopin.api.groups.mapper;

import com.loopin.api.groups.dto.response.GroupMemberResponse;
import com.loopin.api.events.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupMemberMapperTest {

    private GroupMemberMapper groupMemberMapper;

    @BeforeEach
    void setUp() {
        groupMemberMapper = new GroupMemberMapper();
    }

    @Test
    void toResponse_ValidMember_MapsFieldsAndUsesPublicId() {
        GroupMember member = new GroupMember();
        member.setId(777L); // Internal ID
        UUID memberPublicId = UUID.randomUUID();
        member.setPublicId(memberPublicId);
        LocalDateTime joinedAt = LocalDateTime.now();
        member.setJoinedAt(joinedAt);

        EventGroup group = new EventGroup();
        UUID groupPublicId = UUID.randomUUID();
        group.setPublicId(groupPublicId);
        member.setGroup(group);

        User user = new User();
        UUID userPublicId = UUID.randomUUID();
        user.setPublicId(userPublicId);
        member.setUser(user);

        GroupMemberResponse response = groupMemberMapper.toResponse(member);

        assertEquals(memberPublicId, response.getId()); // EXPLICIT CHECK: Uses publicId
        assertEquals(groupPublicId, response.getGroupId()); // EXPLICIT CHECK: Uses publicId
        assertEquals(userPublicId, response.getUserId()); // EXPLICIT CHECK: Uses publicId
        assertEquals(joinedAt, response.getJoinedAt());
    }

    @Test
    void toResponse_NullMember_ReturnsNull() {
        assertNull(groupMemberMapper.toResponse(null));
    }

    @Test
    void toResponseList_PopulatedList_ReturnsMappedList() {
        GroupMember member = new GroupMember();
        member.setPublicId(UUID.randomUUID());
        member.setGroup(new EventGroup());
        member.getGroup().setPublicId(UUID.randomUUID());
        member.setUser(new User());
        member.getUser().setPublicId(UUID.randomUUID());
        member.setJoinedAt(LocalDateTime.now());

        List<GroupMemberResponse> responseList = groupMemberMapper.toResponseList(List.of(member));

        assertEquals(1, responseList.size());
        assertEquals(member.getPublicId(), responseList.get(0).getId());
    }

    @Test
    void toResponseList_NullOrEmptyList_ReturnsEmptyList() {
        List<GroupMemberResponse> nullResult = groupMemberMapper.toResponseList(null);
        assertTrue(nullResult.isEmpty());

        List<GroupMemberResponse> emptyResult = groupMemberMapper.toResponseList(Collections.emptyList());
        assertTrue(emptyResult.isEmpty());
    }
}
