package com.loopin.api.moderation;

import com.loopin.api.common.enums.ContentModerationStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private ContentModerationService serviceWith(String... bannedWords) {
        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setBannedWords(List.of(bannedWords));
        return new ContentModerationService(properties);
    }
}
