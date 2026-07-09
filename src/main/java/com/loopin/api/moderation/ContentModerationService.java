package com.loopin.api.moderation;

import com.loopin.api.common.enums.ContentModerationStatus;
import com.loopin.api.moderation.ai.AiModerationClient;
import com.loopin.api.moderation.ai.AiModerationProperties;
import com.loopin.api.moderation.ai.dto.AiModerationRequest;
import com.loopin.api.moderation.ai.dto.AiModerationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Manual moderation is always the first layer. When explicitly enabled, clean
 * text is subsequently sent to the optional AI client. Any AI failure is
 * fail-open so an external dependency cannot prevent content creation.
 */
@Slf4j
@Service
public class ContentModerationService {

    private final ContentModerationProperties properties;
    private final AiModerationProperties aiProperties;
    private final AiModerationClient aiModerationClient;

    @Autowired
    public ContentModerationService(
            ContentModerationProperties properties,
            AiModerationProperties aiProperties,
            AiModerationClient aiModerationClient
    ) {
        this.properties = properties;
        this.aiProperties = aiProperties;
        this.aiModerationClient = aiModerationClient;
    }

    /** Retained for focused manual-moderation tests and legacy callers. */
    public ContentModerationService(ContentModerationProperties properties) {
        this(properties, new AiModerationProperties(), request -> new AiModerationResponse(false, null));
    }

    public ContentModerationDecision moderate(String... textFields) {
        try {
            Set<String> matchedTerms = new LinkedHashSet<>();
            List<String> bannedTerms = configuredBannedTerms();

            for (String textField : textFields == null ? new String[0] : textFields) {
                String normalizedText = normalize(textField);
                if (normalizedText.isEmpty()) {
                    continue;
                }

                for (String bannedTerm : bannedTerms) {
                    if (containsWholeTerm(normalizedText, bannedTerm)) {
                        matchedTerms.add(bannedTerm);
                    }
                }
            }

            if (!matchedTerms.isEmpty()) {
                return new ContentModerationDecision(
                        ContentModerationStatus.PENDING_REVIEW,
                        List.copyOf(matchedTerms));
            }

            return moderateWithAi(textFields);
        } catch (RuntimeException exception) {
            log.warn("Local content moderation failed; allowing content to preserve availability: {}",
                    exception.getMessage());
            return ContentModerationDecision.approved();
        }
    }

    private ContentModerationDecision moderateWithAi(String... textFields) {
        if (!aiProperties.isEnabled()) {
            return ContentModerationDecision.approved();
        }

        try {
            AiModerationResponse response = aiModerationClient.moderate(
                    new AiModerationRequest(Arrays.stream(textFields == null ? new String[0] : textFields)
                            .filter(Objects::nonNull)
                            .toList()));
            if (response == null || response.risky() == null) {
                throw new IllegalStateException("AI moderation returned an invalid response");
            }
            return Boolean.TRUE.equals(response.risky())
                    ? new ContentModerationDecision(ContentModerationStatus.PENDING_REVIEW, List.of())
                    : ContentModerationDecision.approved();
        } catch (RuntimeException exception) {
            log.warn("AI moderation unavailable; allowing content according to fail-open policy: {}",
                    exception.getMessage());
            return ContentModerationDecision.approved();
        }
    }

    private List<String> configuredBannedTerms() {
        return properties.getBannedWords().stream()
                .map(this::normalize)
                .filter(term -> !term.isEmpty())
                .distinct()
                .toList();
    }

    private boolean containsWholeTerm(String normalizedText, String normalizedTerm) {
        return (" " + normalizedText + " ").contains(" " + normalizedTerm + " ");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }
}
