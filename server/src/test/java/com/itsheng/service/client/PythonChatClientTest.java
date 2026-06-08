package com.itsheng.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.pojo.vo.ChatDailySuggestionsVO;
import com.itsheng.service.config.PythonAiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonChatClientTest {

    private HttpServer server;
    private final List<String> paths = Collections.synchronizedList(new ArrayList<>());
    private final List<String> requestBodies = Collections.synchronizedList(new ArrayList<>());
    private boolean emptySuggestionsResponse;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = responseBody(exchange.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void completeAndDailySuggestionsUseChatBaseUrl() throws Exception {
        PythonAiProperties properties = new PythonAiProperties();
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.setChatBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setChatTimeoutSeconds(5);
        properties.setDailySuggestionsTimeoutSeconds(5);

        PythonChatClient client = new PythonChatClient(properties, new ObjectMapper());

        PythonChatClient.ChatCompletionResult completion = client.complete(
                1L,
                10L,
                "career advice",
                123L,
                List.of(),
                "{\"skills\":[\"Java\"],\"targetRole\":\"backend engineer\"}"
        );
        ChatDailySuggestionsVO suggestions = client.dailySuggestions(1L, 123L, "{\"skills\":[\"Java\"]}");

        assertEquals("stub answer", completion.getContent());
        assertEquals("stub title", completion.getTitle());
        assertEquals(List.of("next question"), completion.getSuggestionQuestions());
        assertEquals(1, suggestions.getSuggestions().size());
        assertEquals("today", suggestions.getSuggestions().get(0).getTitle());
        assertTrue(paths.contains("/api/v1/chat/complete"));
        assertTrue(paths.contains("/api/v1/chat/daily-suggestions"));
        assertTrue(requestBodies.stream().anyMatch(body -> body.contains("\"parsedData\"")));
        assertTrue(requestBodies.stream().anyMatch(body -> body.contains("backend engineer")));
    }

    @Test
    void dailySuggestionsRejectsEmptyResponse() {
        PythonAiProperties properties = new PythonAiProperties();
        properties.setChatBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setDailySuggestionsTimeoutSeconds(5);
        emptySuggestionsResponse = true;

        PythonChatClient client = new PythonChatClient(properties, new ObjectMapper());

        assertThrows(IOException.class, () -> client.dailySuggestions(1L, 123L, "{\"skills\":[\"Java\"]}"));
    }

    private String responseBody(String path) {
        if ("/api/v1/chat/daily-suggestions".equals(path)) {
            if (emptySuggestionsResponse) {
                return "{}";
            }
            return """
                    {
                      "suggestions": [{"title": "today", "text": "rewrite one project"}],
                      "quickQuestions": [{"title": "gap", "text": "what should I improve?"}]
                    }
                    """;
        }
        return """
                {
                  "content": "stub answer",
                  "title": "stub title",
                  "suggestionQuestions": ["next question"]
                }
                """;
    }
}
