package com.loopin.api.groups.updategroupimage;

import com.loopin.api.common.metrics.LoopinOperation;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.loopin.api.media.enums.MediaPurpose.GROUP_IMAGE;

@Component
@RequiredArgsConstructor
public class UpdateGroupImageHandler {

    private final EventGroupRepository groupRepository;
    private final GroupFinder groupFinder;
    private final GroupAdminPolicy adminPolicy;
    private final GroupMembershipPolicy groupMembershipPolicy;
    private final MediaAttachmentManager mediaAttachmentManager;
    private final GroupMapper groupMapper;

    @Transactional
    @LoopinOperation(
        domain = "groups",
        operation = "update_image"
    )
    public GroupResponse handle(
        UpdateGroupImageCommand command
    ) {
        EventGroup group = groupFinder.findGroup(
            command.groupId()
        );

        adminPolicy.requireAdmin(
            group,
            command.currentUsername()
        );

        /*
         * Archived and cancelled groups cannot be edited.
         * The existing group update flow uses the same policy.
         */
        groupMembershipPolicy
            .requireMembershipChangesAllowed(group);

        User currentUser =
            groupFinder.findCurrentUser(
                command.currentUsername()
            );

        MediaAsset updatedImage =
            mediaAttachmentManager.replace(
                group.getImageMedia(),
                command.mediaId(),
                currentUser,
                GROUP_IMAGE
            );

        group.setImageMedia(updatedImage);

        EventGroup savedGroup =
            groupRepository.save(group);

        return groupMapper.toGroupResponse(
            savedGroup
        );
    }
}
