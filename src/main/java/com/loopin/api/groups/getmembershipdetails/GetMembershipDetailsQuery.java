package com.loopin.api.groups.getmembershipdetails;

import java.util.UUID;

public record GetMembershipDetailsQuery(UUID groupId, UUID userId) {
}
