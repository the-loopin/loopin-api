package com.loopin.api.groups.removegroupimage;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.groups.shared.policy.GroupMembershipPolicy;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.loopin.api.media.enums.MediaPurpose.GROUP_IMAGE;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RemoveGroupImageHandlerTest {

    private EventGroupRepository groupRepository;
    private GroupFinder groupFinder;
    private GroupAdminPolicy adminPolicy;
    private GroupMembershipPolicy membershipPolicy;
    private MediaAttachmentManager mediaAttachmentManager;
    private GroupMapper groupMapper;

    private RemoveGroupImageHandler handler;

    @BeforeEach
    void setUp() {
        groupRepository =
            mock(EventGroupRepository.class);

        groupFinder =
            mock(GroupFinder.class);

        adminPolicy =
            mock(GroupAdminPolicy.class);

        membershipPolicy =
            mock(GroupMembershipPolicy.class);

        mediaAttachmentManager =
            mock(MediaAttachmentManager.class);

        groupMapper =
            mock(GroupMapper.class);

        handler = new RemoveGroupImageHandler(
            groupRepository,
            groupFinder,
            adminPolicy,
            membershipPolicy,
            mediaAttachmentManager,
            groupMapper
        );
    }

    @Test
    void handle_validCommand_removesImage() {
        UUID groupId = UUID.randomUUID();

        String currentUsername =
            "admin@loopin.test";

        EventGroup group =
            new EventGroup();

        MediaAsset imageMedia =
            mock(MediaAsset.class);

        group.setImageMedia(imageMedia);

        GroupResponse response =
            mock(GroupResponse.class);

        when(
            groupFinder.findGroup(groupId)
        ).thenReturn(group);

        when(
            groupRepository.save(group)
        ).thenReturn(group);

        when(
            groupMapper.toGroupResponse(group)
        ).thenReturn(response);

        GroupResponse result = handler.handle(
            new RemoveGroupImageCommand(
                groupId,
                currentUsername
            )
        );

        assertSame(response, result);
        assertNull(group.getImageMedia());

        verify(adminPolicy).requireAdmin(
            group,
            currentUsername
        );

        verify(membershipPolicy)
            .requireMembershipChangesAllowed(
                group
            );

        verify(mediaAttachmentManager).remove(
            imageMedia,
            GROUP_IMAGE
        );

        verify(groupRepository).save(group);
    }

    @Test
    void handle_groupWithoutImage_remainsIdempotent() {
        UUID groupId = UUID.randomUUID();

        String currentUsername =
            "admin@loopin.test";

        EventGroup group =
            new EventGroup();

        group.setImageMedia(null);

        when(
            groupFinder.findGroup(groupId)
        ).thenReturn(group);

        when(
            groupRepository.save(group)
        ).thenReturn(group);

        handler.handle(
            new RemoveGroupImageCommand(
                groupId,
                currentUsername
            )
        );

        verify(mediaAttachmentManager).remove(
            null,
            GROUP_IMAGE
        );

        assertNull(group.getImageMedia());
    }

    @Test
    void handle_nonAdmin_stopsBeforeMediaRemoval() {
        UUID groupId = UUID.randomUUID();

        String currentUsername =
            "user@loopin.test";

        EventGroup group =
            new EventGroup();

        when(
            groupFinder.findGroup(groupId)
        ).thenReturn(group);

        doThrow(
            new ForbiddenAccessException(
                "Only the group admin can manage this group"
            )
        ).when(adminPolicy)
            .requireAdmin(
                group,
                currentUsername
            );

        assertThrows(
            ForbiddenAccessException.class,
            () -> handler.handle(
                new RemoveGroupImageCommand(
                    groupId,
                    currentUsername
                )
            )
        );

        verifyNoInteractions(
            mediaAttachmentManager,
            groupRepository,
            groupMapper
        );
    }
}
