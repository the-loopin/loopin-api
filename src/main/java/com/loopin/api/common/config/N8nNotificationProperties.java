package com.loopin.api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "loopin.notifications.n8n")
@Getter
@Setter
public class N8nNotificationProperties {

    private boolean enabled = false;
    private String webhookUrl = "http://localhost:5678/webhook/loopin-notifications";
    private String secret = "";
    private Duration timeout = Duration.ofSeconds(5);
    private int batchSize = 50;
    private Duration maxBackoff = Duration.ofHours(1);
}
