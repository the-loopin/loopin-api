package com.loopin.api.groups.listmygroupjoinrequests;

import com.loopin.api.groups.dto.response.GroupJoinRequestResponse;
import com.loopin.api.groups.repository.GroupJoinRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ListMyGroupJoinRequestsHandler {
    private final GroupJoinRequestRepository requestRepository;

    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponse> handle(ListMyGroupJoinRequestsQuery query) {
        return requestRepository.findByUserId(query.currentUserId()).stream().map(GroupJoinRequestResponse::from).toList();
    }
}
