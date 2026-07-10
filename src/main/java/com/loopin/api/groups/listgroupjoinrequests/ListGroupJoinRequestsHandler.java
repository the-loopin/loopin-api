package com.loopin.api.groups.listgroupjoinrequests;

import com.loopin.api.groups.dto.response.GroupJoinRequestResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import com.loopin.api.groups.shared.joinrequest.GroupJoinRequestFinder;
import com.loopin.api.groups.shared.policy.GroupAdminPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListGroupJoinRequestsHandler {
    private final GroupJoinRequestFinder requestFinder;
    private final GroupJoinRequestRepository requestRepository;
    private final GroupAdminPolicy adminPolicy;

    public List<GroupJoinRequestResponse> handle(ListGroupJoinRequestsQuery query) {
        EventGroup group = requestFinder.findGroup(query.groupId());
        adminPolicy.requireAdmin(group, query.currentUserId());
        return requestRepository.findByGroupId(group.getId()).stream().map(GroupJoinRequestResponse::from).toList();
    }
}
