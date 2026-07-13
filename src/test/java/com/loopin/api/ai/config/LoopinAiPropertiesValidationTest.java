package com.loopin.api.ai.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LoopinAiPropertiesValidationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void workersCannotStartWithoutServiceToken() {
        runner.withPropertyValues("loopin.ai.embedding-jobs.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "loopin.ai.embedding-jobs.batch-size=0",
            "loopin.ai.embedding-jobs.ai-batch-size=129",
            "loopin.ai.embedding-jobs.max-attempts=0",
            "loopin.ai.embedding-jobs.embedding-dimensions=0",
            "loopin.ai.embedding-jobs.initial-backoff=0s",
            "loopin.ai.embedding-jobs.max-backoff=0s",
            "loopin.ai.embedding-jobs.processing-timeout=0s",
            "loopin.ai.embedding-jobs.backoff-jitter=1.1",
            "loopin.ai.embedding-jobs.processing-timeout=2s"
    })
    void invalidWorkerConfigurationFailsStartup(String invalidProperty) {
        runner.withPropertyValues(
                        "loopin.ai.service-token=test-token",
                        "loopin.ai.timeout=2s",
                        invalidProperty)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void validWorkerConfigurationStarts() {
        runner.withPropertyValues(
                        "loopin.ai.service-token=test-token",
                        "loopin.ai.timeout=2s",
                        "loopin.ai.embedding-jobs.processing-timeout=3s",
                        "loopin.ai.embedding-jobs.ai-batch-size=128")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(LoopinAiProperties.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LoopinAiProperties.class)
    static class PropertiesConfiguration { }
}
