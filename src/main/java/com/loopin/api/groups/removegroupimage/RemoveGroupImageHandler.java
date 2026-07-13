package com.loopin.api.groups.removegroupimage;

import com.loopin.api.common.metrics.LoopinOperation;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import com.loopin.api.groups.shared.policy.GroupMembershipPolicy;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.loopin.api.media.enums.MediaPurpose.GROUP_IMAGE;

@Component
@RequiredArgsConstructor
public class RemoveGroupImageHandler {

    private final EventGroupRepository groupRepository;
    private final GroupFinder groupFinder;
    private final GroupAdminPolicy adminPolicy;
    private final GroupMembershipPolicy groupMembershipPolicy;
    private final MediaAttachmentManager mediaAttachmentManager;
    private final GroupMapper groupMapper;

    @Transactional
    @LoopinOperation(
        domain = "groups",
        operation = "remove_image"
    )
    public GroupResponse handle(
        RemoveGroupImageCommand command
    ) {
        EventGroup group = groupFinder.findGroup(
            command.groupId()
        );

        adminPolicy.requireAdmin(
            group,
            command.currentUsername()
        );

        groupMembershipPolicy
            .requireMembershipChangesAllowed(group);

        mediaAttachmentManager.remove(
            group.getImageMedia(),
            GROUP_IMAGE
        );

        group.setImageMedia(null);

        EventGroup savedGroup =
            groupRepository.save(group);

        return groupMapper.toGroupResponse(
            savedGroup
        );
    }
}
