package com.loopin.api.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.loopin.api.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingSafetyTest {

    @Test
    void genericFailureLoggingPreservesSafeDiagnosticsWithoutSensitiveExceptionContent() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        String secret = "Bearer jwt-super-secret-password-api-key";
        MDC.put(CorrelationIdFilter.MDC_KEY, "request-for-safety-test");

        try {
            new GlobalExceptionHandler().handleGenericException(
                new IllegalStateException(secret),
                new MockHttpServletRequest("GET", "/v1/example")
            );

            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain(secret);
                assertThat(event.getFormattedMessage())
                    .contains("requestId=request-for-safety-test")
                    .containsPattern("errorId=[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
                assertThat(event.getMDCPropertyMap())
                    .containsEntry(CorrelationIdFilter.MDC_KEY, "request-for-safety-test");
                assertThat(event.getThrowableProxy()).isNotNull();
                assertThat(event.getThrowableProxy().getMessage()).isEqualTo(IllegalStateException.class.getName());
                assertThat(event.getThrowableProxy().getStackTraceElementProxyArray()).isNotEmpty();
                assertThat(ThrowableProxyUtil.asString(event.getThrowableProxy())).doesNotContain(secret);
            });
        } finally {
            MDC.clear();
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
