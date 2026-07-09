package com.loopin.api.moderation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "moderation")
public class ContentModerationProperties {

    /** Comma-separated in configuration through moderation.banned-words. */
    private List<String> bannedWords = new ArrayList<>();
}
