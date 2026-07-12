package com.loopin.api.groups.api;

import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GroupSubscriptionAuthorizerServiceTest {

    private static final UUID GROUP_PUBLIC_ID = UUID.fromString("a7ca41ab-bfe9-4b4f-b40a-ee66c4fcaaa4");

    private EventGroupRepository groupRepository;
    private GroupMemberRepository memberRepository;
    private GroupSubscriptionAuthorizer authorizer;

    @BeforeEach
    void setUp() {
        groupRepository = mock(EventGroupRepository.class);
        memberRepository = mock(GroupMemberRepository.class);
        authorizer = new GroupSubscriptionAuthorizerService(groupRepository, memberRepository);
    }

    @Test
    void authorizesAnActiveMemberWithoutLeakingTheInternalGroupId() {
        EventGroup group = new EventGroup();
        group.setId(17L);
        when(groupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(memberRepository.existsByGroupIdAndUserId(17L, 42L)).thenReturn(true);

        assertEquals(GroupSubscriptionAuthorization.ALLOWED, authorizer.authorize(GROUP_PUBLIC_ID, 42L));

        verify(memberRepository).existsByGroupIdAndUserId(17L, 42L);
    }

    @Test
    void reportsMissingGroupsDeterministically() {
        when(groupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.empty());

        assertEquals(GroupSubscriptionAuthorization.GROUP_NOT_FOUND, authorizer.authorize(GROUP_PUBLIC_ID, 42L));

        verifyNoInteractions(memberRepository);
    }

    @Test
    void reportsNonMembersDeterministically() {
        EventGroup group = new EventGroup();
        group.setId(17L);
        when(groupRepository.findByPublicId(GROUP_PUBLIC_ID)).thenReturn(Optional.of(group));
        when(memberRepository.existsByGroupIdAndUserId(17L, 42L)).thenReturn(false);

        assertEquals(GroupSubscriptionAuthorization.NOT_A_MEMBER, authorizer.authorize(GROUP_PUBLIC_ID, 42L));
    }
}
