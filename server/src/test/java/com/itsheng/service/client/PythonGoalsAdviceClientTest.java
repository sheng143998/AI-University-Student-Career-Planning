package com.itsheng.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.exception.BaseException;
import com.itsheng.pojo.vo.AiAdviceVO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PythonGoalsAdviceClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesSuccessfulAdviceWithEvidenceAndDiagnostics() throws Exception {
        startServer(200, """
                {"content":"建议补齐 RAG 项目证据","evidenceReferences":[{"sourceType":"milestone","sourceId":"goal_7:chunk:0","reason":"项目里程碑","score":0.95}],"retrievalDiagnostics":{"expandedQueries":["目标拆解"],"metadataFilters":{"userId":"10001","goalId":"7"},"retrieval":"multi_query+bm25+embedding","fusion":"rag_fusion_rrf","reranker":"deterministic_fallback"}}
                """);

        AiAdviceVO advice = clientForServer().generateGoalAdvice(Map.of("requestId", "goals-test"));

        assertEquals("建议补齐 RAG 项目证据", advice.getContent());
        assertEquals("milestone", advice.getEvidenceReferences().get(0).get("sourceType"));
        assertEquals("7", ((Map<?, ?>) advice.getRetrievalDiagnostics().get("metadataFilters")).get("goalId"));
    }

    @Test
    void mapsValidationStatusToContextMessage() throws Exception {
        startServer(400, "{\"message\":\"goal is required\"}");

        BaseException exception = assertThrows(BaseException.class,
                () -> clientForServer().generateGoalAdvice(Map.of()));

        assertEquals("目标AI建议生成失败，请检查目标上下文", exception.getMessage());
    }

    @Test
    void mapsServerErrorToUnavailableMessage() throws Exception {
        startServer(500, "{\"message\":\"boom\"}");

        BaseException exception = assertThrows(BaseException.class,
                () -> clientForServer().generateGoalAdvice(Map.of("requestId", "goals-test")));

        assertEquals("目标AI建议服务暂不可用", exception.getMessage());
    }

    @Test
    void mapsNonJsonResponseToGenericFailure() throws Exception {
        startServer(200, "not-json");

        BaseException exception = assertThrows(BaseException.class,
                () -> clientForServer().generateGoalAdvice(Map.of("requestId", "goals-test")));

        assertEquals("目标AI建议服务暂不可用", exception.getMessage());
    }

    @Test
    void mapsEmptyContentToEmptyMessage() throws Exception {
        startServer(200, "{\"content\":\"\",\"evidenceReferences\":[],\"retrievalDiagnostics\":{}}");

        BaseException exception = assertThrows(BaseException.class,
                () -> clientForServer().generateGoalAdvice(Map.of("requestId", "goals-test")));

        assertEquals("目标AI建议生成结果为空", exception.getMessage());
    }

    @Test
    void mapsTimeoutToTimeoutMessage() throws Exception {
        startServer(200, "{\"content\":\"slow\"}", 1500);
        PythonAiProperties properties = new PythonAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setTimeoutSeconds(1);
        PythonGoalsAdviceClient client = new PythonGoalsAdviceClient(new ObjectMapper(), properties);

        BaseException exception = assertThrows(BaseException.class,
                () -> client.generateGoalAdvice(Map.of("requestId", "goals-test")));

        assertEquals("目标AI建议服务超时，请稍后重试", exception.getMessage());
    }

    private PythonGoalsAdviceClient clientForServer() {
        PythonAiProperties properties = new PythonAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        return new PythonGoalsAdviceClient(new ObjectMapper(), properties);
    }

    private void startServer(int statusCode, String body) throws IOException {
        startServer(statusCode, body, 0);
    }

    private void startServer(int statusCode, String body, long delayMillis) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/goals/advice", exchange -> {
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
    }
}
