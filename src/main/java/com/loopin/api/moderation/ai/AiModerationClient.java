package com.loopin.api.moderation.ai;

import com.loopin.api.moderation.ai.dto.AiModerationRequest;
import com.loopin.api.moderation.ai.dto.AiModerationResponse;

/**
 * Optional boundary for external content-risk classification providers.
 * Implementations may throw when their provider is unavailable; callers must
 * preserve the application's moderation fallback policy.
 */
public interface AiModerationClient {

    AiModerationResponse moderate(AiModerationRequest request);
}
