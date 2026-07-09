package com.loopin.api.moderation;

import com.loopin.api.common.enums.ContentModerationStatus;
import com.loopin.api.moderation.ai.AiModerationClient;
import com.loopin.api.moderation.ai.AiModerationProperties;
import com.loopin.api.moderation.ai.dto.AiModerationResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentModerationServiceTest {

    @Test
    void moderate_CleanText_IsApproved() {
        ContentModerationService service = serviceWith("scam", "offensive phrase");

        assertTrue(service.moderate("Planning a friendly weekend hike").isApproved());
    }

    @Test
    void moderate_BlockedWord_IsPendingReviewCaseInsensitively() {
        ContentModerationService service = serviceWith("scam");

        var decision = service.moderate("This SCAM should be removed.");

        assertEquals(ContentModerationStatus.PENDING_REVIEW, decision.status());
        assertEquals(List.of("scam"), decision.matchedTerms());
    }

    @Test
    void moderate_BlockedPhraseWithPunctuation_IsPendingReview() {
        ContentModerationService service = serviceWith("offensive phrase");

        assertEquals(ContentModerationStatus.PENDING_REVIEW,
                service.moderate("An offensive, phrase is not allowed").status());
    }

    @Test
    void moderate_ConfigurationFailure_AllowsContent() {
        ContentModerationProperties brokenProperties = new ContentModerationProperties() {
            @Override
            public List<String> getBannedWords() {
                throw new IllegalStateException("configuration unavailable");
            }
        };

        assertTrue(new ContentModerationService(brokenProperties)
                .moderate("Safe content should still be accepted")
                .isApproved());
    }

    @Test
    void moderate_AiDisabled_DoesNotCallAiAndAllowsCleanText() {
        AtomicBoolean aiCalled = new AtomicBoolean(false);
        ContentModerationService service = serviceWithAi(false, request -> {
            aiCalled.set(true);
            throw new AssertionError("AI client must not be called while disabled");
        });

        assertTrue(service.moderate("A friendly weekend hike").isApproved());
        assertTrue(!aiCalled.get());
    }

    @Test
    void moderate_AiEnabledRiskyResult_MarksContentPendingReview() {
        ContentModerationService service = serviceWithAi(true,
                request -> new AiModerationResponse(true, "High-risk content"));

        assertEquals(ContentModerationStatus.PENDING_REVIEW,
                service.moderate("Text not matched by the local rules").status());
    }

    @Test
    void moderate_AiFailure_FallsBackToApprovedContent() {
        ContentModerationService service = serviceWithAi(true,
                request -> { throw new IllegalStateException("AI service timed out"); });

        assertTrue(service.moderate("Text not matched by the local rules").isApproved());
    }

    @Test
    void moderate_AiInvalidResponse_FallsBackToApprovedContent() {
        ContentModerationService service = serviceWithAi(true,
                request -> new AiModerationResponse(null, null));

        assertTrue(service.moderate("Text not matched by the local rules").isApproved());
    }

    @Test
    void moderate_ManualMatch_DoesNotDependOnAiAvailability() {
        AtomicBoolean aiCalled = new AtomicBoolean(false);
        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setBannedWords(List.of("scam"));
        AiModerationProperties aiProperties = new AiModerationProperties();
        aiProperties.setEnabled(true);
        ContentModerationService service = new ContentModerationService(properties, aiProperties, request -> {
            aiCalled.set(true);
            throw new IllegalStateException("AI service unavailable");
        });

        var decision = service.moderate("This is a scam");

        assertEquals(ContentModerationStatus.PENDING_REVIEW, decision.status());
        assertEquals(List.of("scam"), decision.matchedTerms());
        assertTrue(!aiCalled.get());
    }

    private ContentModerationService serviceWith(String... bannedWords) {
        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setBannedWords(List.of(bannedWords));
        return new ContentModerationService(properties);
    }

    private ContentModerationService serviceWithAi(boolean enabled, AiModerationClient aiModerationClient) {
        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setBannedWords(List.of("scam"));
        AiModerationProperties aiProperties = new AiModerationProperties();
        aiProperties.setEnabled(enabled);
        return new ContentModerationService(properties, aiProperties, aiModerationClient);
    }
}
