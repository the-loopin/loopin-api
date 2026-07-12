package com.loopin.api.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

    private ExecutorService executor;

    @AfterEach
    void cleanUp() {
        MDC.clear();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void propagatesSubmittingMdcAndRestoresWorkerContextAfterExecution() throws Exception {
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> MDC.put("workerContext", "preserved"))
            .get(5, TimeUnit.SECONDS);

        MDC.put(CorrelationIdFilter.MDC_KEY, "request-async-123");
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        AtomicReference<Map<String, String>> taskContext = new AtomicReference<>();
        executor.submit(decorator.decorate(() -> taskContext.set(MDC.getCopyOfContextMap())))
            .get(5, TimeUnit.SECONDS);
        MDC.clear();

        Map<String, String> workerContextAfterTask = executor.submit(MDC::getCopyOfContextMap)
            .get(5, TimeUnit.SECONDS);

        assertThat(taskContext.get()).containsEntry(CorrelationIdFilter.MDC_KEY, "request-async-123")
            .doesNotContainKey("workerContext");
        assertThat(workerContextAfterTask).containsEntry("workerContext", "preserved")
            .doesNotContainKey(CorrelationIdFilter.MDC_KEY);
    }
}
