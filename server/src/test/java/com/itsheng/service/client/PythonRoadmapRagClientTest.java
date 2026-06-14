package com.itsheng.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.service.config.PythonAiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonRoadmapRagClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generatePersonalizedRecommendationsReturnsRawJson() throws Exception {
        String body = "{\"lateralPaths\":[],\"diagnostics\":{\"fusion\":\"rrf\"}}";
        startServer(200, body);
        PythonRoadmapRagClient client = new PythonRoadmapRagClient(properties(), new ObjectMapper());

        JsonNode result = client.generatePersonalizedRecommendations(Map.of("userId", 7));

        assertTrue(result.has("lateralPaths"));
        assertEquals("rrf", result.get("diagnostics").get("fusion").asText());
    }

    @Test
    void generatePersonalizedRecommendationsMapsHttpErrors() throws Exception {
        startServer(500, "{\"message\":\"down\"}");
        PythonRoadmapRagClient client = new PythonRoadmapRagClient(properties(), new ObjectMapper());

        PythonRoadmapRagClient.PythonRoadmapRagException ex = assertThrows(
                PythonRoadmapRagClient.PythonRoadmapRagException.class,
                () -> client.generatePersonalizedRecommendations(Map.of("userId", 7))
        );

        assertEquals(500, ex.getStatusCode());
        assertTrue(ex.getResponseBody().contains("down"));
    }

    @Test
    void generatePersonalizedRecommendationsMapsInvalidJsonAsUnavailable() throws Exception {
        startServer(200, "{not-json");
        PythonRoadmapRagClient client = new PythonRoadmapRagClient(properties(), new ObjectMapper());

        assertThrows(PythonRoadmapRagClient.PythonRoadmapRagUnavailableException.class,
                () -> client.generatePersonalizedRecommendations(Map.of("userId", 7)));
    }

    @Test
    void generatePersonalizedRecommendationsMapsTimeout() throws Exception {
        startDelayedServer(2500);
        PythonRoadmapRagClient client = new PythonRoadmapRagClient(properties(1), new ObjectMapper());

        assertThrows(PythonRoadmapRagClient.PythonRoadmapRagTimeoutException.class,
                () -> client.generatePersonalizedRecommendations(Map.of("userId", 7)));
    }

    @Test
    void generatePersonalizedRecommendationsMapsConnectionFailureAsUnavailable() throws Exception {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        PythonAiProperties properties = new PythonAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + unusedPort);
        properties.setRoadmapTimeoutSeconds(1);
        PythonRoadmapRagClient client = new PythonRoadmapRagClient(properties, new ObjectMapper());

        assertThrows(PythonRoadmapRagClient.PythonRoadmapRagUnavailableException.class,
                () -> client.generatePersonalizedRecommendations(Map.of("userId", 7)));
    }

    private PythonAiProperties properties() {
        return properties(2);
    }

    private PythonAiProperties properties(int timeoutSeconds) {
        PythonAiProperties properties = new PythonAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setRoadmapTimeoutSeconds(timeoutSeconds);
        return properties;
    }

    private void startServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/roadmap/recommendations/personalized", exchange -> {
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    private void startDelayedServer(long delayMillis) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/roadmap/recommendations/personalized", exchange -> {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] response = "{\"lateralPaths\":[],\"diagnostics\":{}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }
}
