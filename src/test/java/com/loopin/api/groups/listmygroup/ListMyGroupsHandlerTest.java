package com.loopin.api.groups.listmygroups;

import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.GroupMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMyGroupsHandlerTest {

    @Mock
    private GroupMemberRepository memberRepository;

    @Mock
    private GroupMapper groupMapper;

    @InjectMocks
    private ListMyGroupsHandler handler;

    @Test
    void handle_returnsGroupsCurrentUserBelongsTo() {
        String currentUsername =
            "member@loopin.test";

        EventGroup firstGroup =
            new EventGroup();

        EventGroup secondGroup =
            new EventGroup();

        GroupResponse firstResponse =
            org.mockito.Mockito.mock(
                GroupResponse.class
            );

        GroupResponse secondResponse =
            org.mockito.Mockito.mock(
                GroupResponse.class
            );

        when(
            memberRepository.findGroupsByUserEmail(
                currentUsername
            )
        ).thenReturn(
            List.of(
                firstGroup,
                secondGroup
            )
        );

        when(
            groupMapper.toGroupResponse(
                firstGroup
            )
        ).thenReturn(firstResponse);

        when(
            groupMapper.toGroupResponse(
                secondGroup
            )
        ).thenReturn(secondResponse);

        List<GroupResponse> result =
            handler.handle(
                new ListMyGroupsQuery(
                    currentUsername
                )
            );

        assertThat(result)
            .containsExactly(
                firstResponse,
                secondResponse
            );

        verify(memberRepository)
            .findGroupsByUserEmail(
                currentUsername
            );

        verify(groupMapper)
            .toGroupResponse(firstGroup);

        verify(groupMapper)
            .toGroupResponse(secondGroup);
    }

    @Test
    void handle_userWithoutGroups_returnsEmptyList() {
        String currentUsername =
            "member@loopin.test";

        when(
            memberRepository.findGroupsByUserEmail(
                currentUsername
            )
        ).thenReturn(List.of());

        List<GroupResponse> result =
            handler.handle(
                new ListMyGroupsQuery(
                    currentUsername
                )
            );

        assertThat(result).isEmpty();

        verify(memberRepository)
            .findGroupsByUserEmail(
                currentUsername
            );

        verifyNoInteractions(groupMapper);
    }
}
