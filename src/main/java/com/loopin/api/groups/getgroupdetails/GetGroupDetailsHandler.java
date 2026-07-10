package com.loopin.api.groups.getgroupdetails;

import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.shared.finder.GroupFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetGroupDetailsHandler {
    private final GroupFinder groupFinder;
    private final GroupMapper groupMapper;

    public GroupResponse handle(GetGroupDetailsQuery query) {
        return groupMapper.toGroupResponse(groupFinder.findGroup(query.groupId()));
    }
}
