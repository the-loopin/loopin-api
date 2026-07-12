package com.loopin.api.common.architecturefixture;

import com.loopin.api.groups.repository.GroupMemberRepository;

/** Deliberately forbidden fixture used to prove the Groups boundary rule. */
public class ForbiddenGroupsRepositoryDependency {
    private GroupMemberRepository groupMemberRepository;
}
