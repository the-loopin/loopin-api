package com.loopin.api.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.loopin.api.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingSafetyTest {

    @Test
    void genericFailureLoggingDoesNotIncludeSensitiveExceptionContent() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        String secret = "Bearer jwt-super-secret-password-api-key";

        try {
            new GlobalExceptionHandler().handleGenericException(
                new IllegalStateException(secret),
                new MockHttpServletRequest("GET", "/v1/example")
            );

            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain(secret);
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
