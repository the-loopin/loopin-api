package com.loopin.api.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(LoopinOperationMetricsIntegrationTest.InstrumentedOperationConfiguration.class)
class LoopinOperationMetricsIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private InstrumentedOperation instrumentedOperation;

    @Test
    void recordsASuccessCounterThroughTheAopInstrumentation() {
        Counter counter = operationCounter("success");
        double countBefore = counter.count();

        instrumentedOperation.succeed();

        assertEquals(countBefore + 1, counter.count());
    }

    @Test
    void recordsAFailureCounterThroughTheAopInstrumentation() {
        assertThrows(IllegalStateException.class, () -> instrumentedOperation.fail());
        Counter counter = operationCounter("failure");
        double countBefore = counter.count();

        assertThrows(IllegalStateException.class, () -> instrumentedOperation.fail());

        assertEquals(countBefore + 1, counter.count());
    }

    private Counter operationCounter(String outcome) {
        Counter counter = meterRegistry.find("loopin.operations")
            .tags(
                "domain", "events",
                "operation", "create",
                "outcome", outcome
            )
            .counter();
        assertNotNull(counter);
        return counter;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class InstrumentedOperationConfiguration {

        @Bean
        InstrumentedOperation instrumentedOperation() {
            return new InstrumentedOperation();
        }
    }

    static class InstrumentedOperation {

        @LoopinOperation(domain = "events", operation = "create")
        public void succeed() {
        }

        @LoopinOperation(domain = "events", operation = "create")
        public void fail() {
            throw new IllegalStateException("expected test failure");
        }
    }
}
