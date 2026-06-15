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
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PythonResumeAiClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void analyzeReturnsValidatedJson() throws Exception {
        PythonResumeAiClient client = clientFor(json(200, successBody()));

        assertEquals("completed", client.analyze(payload()).path("status").asText());
    }

    @Test
    void ocrPageReturnsValidatedData() throws Exception {
        PythonResumeAiClient client = clientFor(json(200, successBody()), json(200, ocrSuccessBody()));

        assertEquals("Python RAG", client.ocrPage(Map.of("image_data_url", "data:image/png;base64,abc")).path("text").asText());
    }

    @Test
    void ocrPageRejectsFailureCode() throws Exception {
        PythonResumeAiClient client = clientFor(json(200, successBody()), json(200, "{\"code\":0,\"msg\":\"bad\",\"data\":{}}"));

        assertThrows(PythonResumeAiClient.PythonResumeSchemaException.class,
                () -> client.ocrPage(Map.of("image_data_url", "data:image/png;base64,abc")));
    }

    @Test
    void analyzeMapsHttpFailure() throws Exception {
        PythonResumeAiClient client = clientFor(json(502, "{\"message\":\"bad gateway\"}"));

        PythonResumeAiClient.PythonResumeHttpException ex = assertThrows(
                PythonResumeAiClient.PythonResumeHttpException.class,
                () -> client.analyze(payload())
        );
        assertEquals(502, ex.getStatusCode());
    }

    @Test
    void analyzeRejectsEmptyBody() throws Exception {
        PythonResumeAiClient client = clientFor(json(200, ""));

        assertThrows(PythonResumeAiClient.PythonResumeSchemaException.class, () -> client.analyze(payload()));
    }

    @Test
    void analyzeRejectsInvalidJson() throws Exception {
        PythonResumeAiClient client = clientFor(json(200, "{"));

        assertThrows(PythonResumeAiClient.PythonResumeUnavailableException.class, () -> client.analyze(payload()));
    }

    @Test
    void analyzeRejectsSchemaMissingFields() throws Exception {
        PythonResumeAiClient client = clientFor(json(200, "{\"status\":\"completed\"}"));

        assertThrows(PythonResumeAiClient.PythonResumeSchemaException.class, () -> client.analyze(payload()));
    }

    @Test
    void analyzeMapsTimeout() throws Exception {
        PythonResumeAiClient client = clientFor(exchange -> {
            sleep(1200);
            write(exchange, 200, successBody());
        });
        ReflectionTestUtils.setField(clientProperties(client), "resumeTimeoutSeconds", 1);

        assertThrows(PythonResumeAiClient.PythonResumeTimeoutException.class, () -> client.analyze(payload()));
    }

    private PythonResumeAiClient clientFor(Handler handler) throws IOException {
        return clientFor(handler, handler);
    }

    private PythonResumeAiClient clientFor(Handler analyzeHandler, Handler ocrHandler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/resume/analyze", analyzeHandler::handle);
        server.createContext("/internal/resume/ocr", ocrHandler::handle);
        server.start();
        PythonAiProperties properties = new PythonAiProperties();
        ReflectionTestUtils.setField(properties, "resumeBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        ReflectionTestUtils.setField(properties, "resumeTimeoutSeconds", 5);
        ReflectionTestUtils.setField(properties, "resumeOcrTimeoutSeconds", 5);
        return new PythonResumeAiClient(properties, new ObjectMapper());
    }

    private PythonAiProperties clientProperties(PythonResumeAiClient client) {
        return (PythonAiProperties) ReflectionTestUtils.getField(client, "properties");
    }

    private Handler json(int status, String body) {
        return exchange -> write(exchange, status, body);
    }

    private void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private Map<String, Object> payload() {
        return Map.of(
                "vector_store_id", "vs-1",
                "user_id", 1001,
                "resume_text", "Python RAG project",
                "file_type", "txt",
                "original_file_name", "resume.txt",
                "resume_file_path", "oss://resume.txt",
                "metadata", Map.of("visibility", "user")
        );
    }

    private String successBody() {
        return """
                {
                  "status": "completed",
                  "parsed_data": {"target_role": "AI Agent Intern", "skills": ["Python"]},
                  "scores": {"keyword_match": 80, "layout": 75, "skill_depth": 82, "experience": 70},
                  "highlights": ["RAG evidence"],
                  "suggestions": [{"type": "SKILL", "content": "Add metrics"}],
                  "capability_profile": {
                    "overall_score": 80,
                    "completeness_score": 75,
                    "competitiveness_score": 78,
                    "capability_scores": {"professional_skill": 80},
                    "professional_skills": [{"name": "Python"}],
                    "certificates": [],
                    "soft_skills": {"learning": {"score": 80}},
                    "ai_evaluation": "fallback"
                  },
                  "rag_diagnostics": {
                    "retrieval": {"bm25": true, "embedding_fallback": "hash", "fusion": "rrf", "reranker": "deterministic"}
                  }
                }
                """;
    }

    private String ocrSuccessBody() {
        return """
                {
                  "code": 1,
                  "msg": "success",
                  "data": {"text": "Python RAG", "model": "mock-ocr"}
                }
                """;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
