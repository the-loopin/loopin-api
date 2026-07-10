package com.loopin.api.groups.getgroupdetails;

import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.shared.finder.GroupFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetGroupDetailsHandler {
    private final GroupFinder groupFinder;
    private final GroupMapper groupMapper;

    @Transactional(readOnly = true)
    public GroupResponse handle(GetGroupDetailsQuery query) {
        return groupMapper.toGroupResponse(groupFinder.findGroup(query.groupId()));
    }
}
