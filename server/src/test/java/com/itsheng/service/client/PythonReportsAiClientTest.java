package com.itsheng.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.service.config.PythonAiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class PythonReportsAiClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void mapsSuccessfulResponse() throws Exception {
        PythonReportsAiClient client = newClient(200, """
                {"status":"OK","aiSuggestions":"focus backend metrics","evidenceRefs":[{"id":"e1"}],"ragDiagnostics":{"status":"OK"}}
                """, 0);

        PythonReportsAiClient.ReportsSupportResult result = client.generateSupport(Map.of("reportId", 1, "userId", 2));

        assertEquals("OK", result.status());
        assertEquals("focus backend metrics", result.aiSuggestions());
        assertEquals("e1", result.evidenceRefs().get(0).path("id").asText());
        assertEquals("OK", result.ragDiagnostics().path("status").asText());
    }

    @Test
    void mapsEmptyRetrievalResponse() throws Exception {
        PythonReportsAiClient client = newClient(200, """
                {"status":"EMPTY_RETRIEVAL","aiSuggestions":"","evidenceRefs":[],"ragDiagnostics":{"status":"EMPTY_RETRIEVAL","emptyRetrieval":true}}
                """, 0);

        PythonReportsAiClient.ReportsSupportResult result = client.generateSupport(Map.of("reportId", 1, "userId", 2));

        assertEquals("EMPTY_RETRIEVAL", result.status());
        assertTrue(result.evidenceRefs().isArray());
        assertTrue(result.ragDiagnostics().path("emptyRetrieval").asBoolean());
    }

    @Test
    void throwsForValidationErrorWithoutRetry() throws Exception {
        CountingHandler handler = startServer(400, "{\"error\":\"VALIDATION_ERROR\"}", 0);
        PythonReportsAiClient client = newClient();

        PythonReportsAiClient.PythonReportsClientException ex = assertThrows(
                PythonReportsAiClient.PythonReportsClientException.class,
                () -> client.generateSupport(Map.of("reportId", 1, "userId", 2))
        );

        assertEquals(400, ex.getStatusCode());
        assertEquals(1, handler.calls);
    }

    @Test
    void throwsForServerErrorWithoutRetry() throws Exception {
        CountingHandler handler = startServer(500, "{\"error\":\"INTERNAL_ERROR\"}", 0);
        PythonReportsAiClient client = newClient();

        assertThrows(PythonReportsAiClient.PythonReportsClientException.class,
                () -> client.generateSupport(Map.of("reportId", 1, "userId", 2)));

        assertEquals(1, handler.calls);
    }

    @Test
    void throwsForInvalidJsonBody() throws Exception {
        PythonReportsAiClient client = newClient(200, "not-json", 0);

        assertThrows(PythonReportsAiClient.PythonReportsInvalidResponseException.class,
                () -> client.generateSupport(Map.of("reportId", 1, "userId", 2)));
    }

    @Test
    void throwsForMissingRequiredResponseFields() throws Exception {
        PythonReportsAiClient client = newClient(200, "{}", 0);

        assertThrows(PythonReportsAiClient.PythonReportsInvalidResponseException.class,
                () -> client.generateSupport(Map.of("reportId", 1, "userId", 2)));
    }

    @Test
    void throwsForUnsupportedStatus() throws Exception {
        PythonReportsAiClient client = newClient(200, """
                {"status":"PARTIAL","aiSuggestions":"","evidenceRefs":[],"ragDiagnostics":{"status":"PARTIAL"}}
                """, 0);

        assertThrows(PythonReportsAiClient.PythonReportsInvalidResponseException.class,
                () -> client.generateSupport(Map.of("reportId", 1, "userId", 2)));
    }

    @Test
    void throwsForWrongFieldTypes() throws Exception {
        PythonReportsAiClient client = newClient(200, """
                {"status":"OK","aiSuggestions":123,"evidenceRefs":{},"ragDiagnostics":[]}
                """, 0);

        assertThrows(PythonReportsAiClient.PythonReportsInvalidResponseException.class,
                () -> client.generateSupport(Map.of("reportId", 1, "userId", 2)));
    }

    @Test
    void throwsForTimeout() throws Exception {
        PythonReportsAiClient client = newClient(200, "{}", 2_000);

        assertThrows(PythonReportsAiClient.PythonReportsTimeoutException.class,
                () -> client.generateSupport(Map.of("reportId", 1, "userId", 2)));
    }

    private PythonReportsAiClient newClient(int status, String body, long delayMillis) throws IOException {
        startServer(status, body, delayMillis);
        return newClient();
    }

    private PythonReportsAiClient newClient() {
        PythonAiProperties properties = new PythonAiProperties();
        properties.setReportsBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setReportsTimeoutSeconds(1);
        return new PythonReportsAiClient(properties, new ObjectMapper());
    }

    private CountingHandler startServer(int status, String body, long delayMillis) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountingHandler handler = new CountingHandler(status, body, delayMillis);
        server.createContext("/api/v1/reports/generate-support", exchange -> {
            handler.calls++;
            if (handler.delayMillis > 0) {
                try {
                    Thread.sleep(handler.delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] response = handler.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(handler.status, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        return handler;
    }

    private static class CountingHandler {
        final int status;
        final String body;
        final long delayMillis;
        int calls;

        CountingHandler(int status, String body, long delayMillis) {
            this.status = status;
            this.body = body;
            this.delayMillis = delayMillis;
        }
    }
}
