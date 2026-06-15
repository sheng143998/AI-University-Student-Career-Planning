package com.itsheng.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.service.config.PythonAiProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PythonMarketAiClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void classifyJobReturnsValidatedData() throws Exception {
        startServer("/internal/market/jobs/classify", 200, """
                {"code":1,"msg":"success","data":{"category_code":"AI_APP_JUNIOR","category_name":"AI Application Engineer"}}
                """);
        PythonMarketAiClient client = clientForServer();

        assertEquals("AI_APP_JUNIOR", client.classifyJob(Map.of("job_content", "Python RAG")).path("category_code").asText());
    }

    @Test
    void indexJobsParsesRecords() throws Exception {
        startServer("/internal/market/jobs/index", 200, """
                {"code":1,"msg":"success","data":{"records":[{"id":"job-1","job_id":1,"embedding":"[0.1]","metadata":{"source":"test"},"content_hash":"abc"}],"diagnostics":{"chunking":"recursive"}}}
                """);
        PythonMarketAiClient client = clientForServer();

        PythonMarketAiClient.JobIndexResult result = client.indexJobs(Map.of("jobs", java.util.List.of()));

        assertEquals(1, result.records().size());
        assertEquals("job-1", result.records().get(0).id());
        assertEquals("{\"source\":\"test\"}", result.records().get(0).metadata());
        assertEquals("recursive", result.diagnostics().path("chunking").asText());
    }

    @Test
    void searchJobsParsesRankedIds() throws Exception {
        startServer("/internal/market/jobs/search", 200, """
                {"code":1,"msg":"success","data":{"job_ids":[2,1],"scores":[],"retrieval":{"fusion_method":"rrf"}}}
                """);
        PythonMarketAiClient client = clientForServer();

        PythonMarketAiClient.JobSearchResult result = client.searchJobs(Map.of("query_text", "Python"));

        assertEquals(java.util.List.of(2L, 1L), result.jobIds());
        assertEquals("rrf", result.retrieval().path("fusion_method").asText());
    }

    @Test
    void mapsValidationResponse() throws Exception {
        startServer("/internal/market/jobs/classify", 422, "{\"message\":\"bad\"}");
        PythonMarketAiClient client = clientForServer();

        assertThrows(PythonMarketAiClient.PythonMarketValidationException.class,
                () -> client.classifyJob(Map.of("job_content", "")));
    }

    @Test
    void mapsCodeZeroResponse() throws Exception {
        startServer("/internal/market/jobs/classify", 200, "{\"code\":0,\"msg\":\"VALIDATION_ERROR\",\"data\":{}}");
        PythonMarketAiClient client = clientForServer();

        assertThrows(PythonMarketAiClient.PythonMarketValidationException.class,
                () -> client.classifyJob(Map.of("job_content", "")));
    }

    private PythonMarketAiClient clientForServer() {
        PythonAiProperties properties = new PythonAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        ReflectionTestUtils.setField(properties, "marketTimeoutSeconds", 5);
        return new PythonMarketAiClient(properties, new ObjectMapper());
    }

    private void startServer(String path, int statusCode, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> write(exchange, statusCode, body));
        server.start();
    }

    private void write(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}
