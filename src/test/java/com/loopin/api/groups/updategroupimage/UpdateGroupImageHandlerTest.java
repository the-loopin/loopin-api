package com.loopin.api.groups.updategroupimage;

import com.loopin.api.common.exception.ForbiddenAccessException;
import com.loopin.api.common.exception.InvalidGroupStateException;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.groups.shared.policy.GroupMembershipPolicy;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.loopin.api.media.enums.MediaPurpose.GROUP_IMAGE;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UpdateGroupImageHandlerTest {

    private EventGroupRepository groupRepository;
    private GroupFinder groupFinder;
    private GroupAdminPolicy adminPolicy;
    private GroupMembershipPolicy membershipPolicy;
    private MediaAttachmentManager mediaAttachmentManager;
    private GroupMapper groupMapper;

    private UpdateGroupImageHandler handler;

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

        handler = new UpdateGroupImageHandler(
            groupRepository,
            groupFinder,
            adminPolicy,
            membershipPolicy,
            mediaAttachmentManager,
            groupMapper
        );
    }

    @Test
    void handle_validCommand_replacesImage() {
        UUID groupId = UUID.randomUUID();
        UUID newMediaId = UUID.randomUUID();

        String currentUsername =
            "admin@loopin.test";

        EventGroup group =
            new EventGroup();

        MediaAsset oldImage =
            mock(MediaAsset.class);

        MediaAsset newImage =
            mock(MediaAsset.class);

        group.setImageMedia(oldImage);

        User currentUser =
            new User(
                currentUsername,
                "Admin",
                null
            );

        GroupResponse response =
            mock(GroupResponse.class);

        when(
            groupFinder.findGroup(groupId)
        ).thenReturn(group);

        when(
            groupFinder.findCurrentUser(
                currentUsername
            )
        ).thenReturn(currentUser);

        when(
            mediaAttachmentManager.replace(
                oldImage,
                newMediaId,
                currentUser,
                GROUP_IMAGE
            )
        ).thenReturn(newImage);

        when(
            groupRepository.save(group)
        ).thenReturn(group);

        when(
            groupMapper.toGroupResponse(group)
        ).thenReturn(response);

        GroupResponse result = handler.handle(
            new UpdateGroupImageCommand(
                groupId,
                newMediaId,
                currentUsername
            )
        );

        assertSame(response, result);

        assertSame(
            newImage,
            group.getImageMedia()
        );

        verify(adminPolicy).requireAdmin(
            group,
            currentUsername
        );

        verify(membershipPolicy)
            .requireMembershipChangesAllowed(
                group
            );

        verify(mediaAttachmentManager).replace(
            oldImage,
            newMediaId,
            currentUser,
            GROUP_IMAGE
        );

        verify(groupRepository).save(group);
    }

    @Test
    void handle_nonAdmin_stopsBeforeMediaReplacement() {
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
                new UpdateGroupImageCommand(
                    groupId,
                    UUID.randomUUID(),
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

    @Test
    void handle_archivedGroup_stopsBeforeMediaReplacement() {
        UUID groupId = UUID.randomUUID();

        String currentUsername =
            "admin@loopin.test";

        EventGroup group =
            new EventGroup();

        when(
            groupFinder.findGroup(groupId)
        ).thenReturn(group);

        doThrow(
            new InvalidGroupStateException(
                "Group is archived"
            )
        ).when(membershipPolicy)
            .requireMembershipChangesAllowed(
                group
            );

        assertThrows(
            InvalidGroupStateException.class,
            () -> handler.handle(
                new UpdateGroupImageCommand(
                    groupId,
                    UUID.randomUUID(),
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
