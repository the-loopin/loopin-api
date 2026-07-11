package com.loopin.api.media.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "loopin.storage")
public class StorageProperties {

    private String endpoint;
    private String presignEndpoint;
    private String region;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess = true;
    private Duration uploadUrlTtl = Duration.ofMinutes(10);
}
