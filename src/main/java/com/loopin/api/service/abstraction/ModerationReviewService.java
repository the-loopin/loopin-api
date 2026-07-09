package com.loopin.api.service.abstraction;

import com.loopin.api.dto.moderation.response.ModerationItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ModerationReviewService {

    Page<ModerationItemResponse> getPendingItems(Pageable pageable);

    ModerationItemResponse approveEvent(UUID eventId, String adminEmail);

    ModerationItemResponse rejectEvent(UUID eventId, String reason, String adminEmail);
}
