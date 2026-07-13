package com.loopin.api.ai.client;

import com.loopin.api.ai.config.LoopinAiProperties;
import com.loopin.api.common.logging.CorrelationIdFilter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoopinAiClientHttpContractTest {
    private HttpServer server;
    private final List<CapturedRequest> requests = new ArrayList<>();
    private LoopinAiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::respond);
        server.start();

        LoopinAiProperties properties = new LoopinAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setTimeout(Duration.ofSeconds(2));
        properties.setServiceToken("contract-test-token");
        client = new LoopinAiClient(properties);
        MDC.put(CorrelationIdFilter.MDC_KEY, "contract-request-123");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        server.stop(0);
    }

    @Test
    void authenticatedSingleAndBatchRequestsUseExactPathsAndPropagateRequestId() {
        assertThat(client.embedPassage("single").embedding()).containsExactly(0.1, 0.2);
        assertThat(client.embedPassages(List.of("first", "second")).embeddings()).hasSize(2);

        assertThat(requests).extracting(CapturedRequest::path)
                .containsExactly("/v1/embeddings/text", "/v1/embeddings/batch");
        assertThat(requests).allSatisfy(request -> {
            assertThat(request.authorization()).isEqualTo("Bearer contract-test-token");
            assertThat(request.requestId()).isEqualTo("contract-request-123");
            assertThat(request.body()).doesNotContain("contract-test-token");
        });
        assertThat(requests.get(1).body())
                .contains("\"texts\":[\"first\",\"second\"]")
                .contains("\"input_type\":\"passage\"");
    }

    private void respond(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        requests.add(new CapturedRequest(path,
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst(CorrelationIdFilter.HEADER_NAME),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
        String response = path.equals("/v1/embeddings/batch")
                ? "{\"model\":\"model\",\"dimensions\":2,\"embeddings\":[[0.1,0.2],[0.3,0.4]]}"
                : "{\"model\":\"model\",\"dimensions\":2,\"embedding\":[0.1,0.2]}";
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record CapturedRequest(String path, String authorization, String requestId, String body) { }
}
