package com.loopin.api.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralizes custom metric names and their bounded tag values. In particular, no user,
 * event, group, media, email, session, or request identifier may be used as a metric tag.
 */
@Component
public class LoopinMetrics {

    private static final List<Operation> CATALOG = List.of(
        new Operation("events", "create"),
        new Operation("events", "loop_in"),
        new Operation("groups", "create"),
        new Operation("groups", "add_member"),
        new Operation("notifications", "create"),
        new Operation("websocket", "message"),
        new Operation("ai", "embed_passage"),
        new Operation("ai", "embed_query"),
        new Operation("ai", "rerank"),
        new Operation("media", "request_upload"),
        new Operation("media", "complete_upload"),
        new Operation("media", "delete")
    );

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeWebSocketConnections = new AtomicInteger();

    public LoopinMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("loopin.websocket.connections.active", activeWebSocketConnections, AtomicInteger::get)
            .description("Active STOMP WebSocket connections")
            .register(meterRegistry);

        // Register known operation series up front so a fresh instance is observable before
        // the first business request. The catalog only contains finite, code-owned labels.
        CATALOG.forEach(operation -> {
            operationCounter(operation, "success");
            operationTimer(operation, "success");
        });
    }

    public void recordOperation(String domain, String operation, boolean success, Duration duration) {
        recordOperations(domain, operation, success, 1, duration);
    }

    /**
     * Records a finite batch of successful domain operations without adding a batch-specific
     * identifier or label. Failures are represented by one failed batch operation.
     */
    public void recordOperations(
        String domain,
        String operation,
        boolean success,
        long count,
        Duration duration
    ) {
        Operation metricOperation = new Operation(domain, operation);
        String outcome = success ? "success" : "failure";
        if (count > 0) {
            operationCounter(metricOperation, outcome).increment(count);
        }
        operationTimer(metricOperation, outcome).record(duration);
    }

    public void webSocketConnected() {
        activeWebSocketConnections.incrementAndGet();
        Counter.builder("loopin.websocket.sessions")
            .description("Established STOMP WebSocket sessions")
            .register(meterRegistry)
            .increment();
    }

    public void webSocketDisconnected(Duration duration) {
        activeWebSocketConnections.updateAndGet(current -> Math.max(0, current - 1));
        Timer.builder("loopin.websocket.session")
            .description("Duration of completed STOMP WebSocket sessions")
            .register(meterRegistry)
            .record(duration);
    }

    private Counter operationCounter(Operation operation, String outcome) {
        return Counter.builder("loopin.operations")
            .description("Completed Loopin business operations")
            .tags("domain", operation.domain(), "operation", operation.name(), "outcome", outcome)
            .register(meterRegistry);
    }

    private Timer operationTimer(Operation operation, String outcome) {
        return Timer.builder("loopin.operation")
            .description("Duration of Loopin business operations")
            .tags("domain", operation.domain(), "operation", operation.name(), "outcome", outcome)
            .register(meterRegistry);
    }

    private record Operation(String domain, String name) {
    }
}
