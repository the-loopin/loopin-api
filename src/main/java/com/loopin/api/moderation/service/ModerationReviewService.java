package com.loopin.api.moderation.service;

import com.loopin.api.moderation.dto.response.ModerationItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ModerationReviewService {

    Page<ModerationItemResponse> getPendingItems(Pageable pageable);

    ModerationItemResponse approveEvent(UUID eventId, String adminEmail);

    ModerationItemResponse rejectEvent(UUID eventId, String reason, String adminEmail);
}
