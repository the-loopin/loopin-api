package com.loopin.api.moderation;

import com.loopin.api.common.enums.ContentModerationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Local, deterministic moderation for user-provided text. This deliberately
 * has no network or AI dependency so it can be used on synchronous write
 * paths. A configuration or matching failure is fail-open to protect the
 * application availability; failures are logged for operators.
 */
@Slf4j
@Service
public class ContentModerationService {

    private final ContentModerationProperties properties;

    public ContentModerationService(ContentModerationProperties properties) {
        this.properties = properties;
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

            return matchedTerms.isEmpty()
                    ? ContentModerationDecision.approved()
                    : new ContentModerationDecision(
                            ContentModerationStatus.PENDING_REVIEW,
                            List.copyOf(matchedTerms));
        } catch (RuntimeException exception) {
            log.warn("Local content moderation failed; allowing content to preserve availability: {}",
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
