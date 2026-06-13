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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PythonDashboardAiClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void usesDefaultBaseUrlAndDashboardTimeout() {
        PythonAiProperties properties = new PythonAiProperties();

        assertEquals("http://127.0.0.1:8090", properties.getBaseUrl());
        assertEquals(30, properties.getDashboardTimeoutSeconds());
    }

    @Test
    void parsesSuccessfulTargetJobMatch() throws Exception {
        startServer(200, """
                {"code":1,"msg":"success","data":{"matched_job":{"job_id":101,"job_name":"AI算法工程师","score":0.87},"retrieval":{"fusion_method":"rrf"}}}
                """);
        PythonDashboardAiClient client = clientForServer();

        PythonDashboardAiClient.TargetJobMatchResult result = client.matchTargetJob(Map.of("request_id", "rid"));

        assertEquals(101L, result.jobId());
        assertEquals("AI算法工程师", result.jobName());
        assertEquals(0.87, result.score());
        assertEquals("rrf", result.diagnostics().path("fusion_method").asText());
    }

    @Test
    void mapsNoMatchToBusinessMessage() throws Exception {
        startServer(200, """
                {"code":0,"msg":"NO_MATCH","data":{"retrieval":{"candidate_count":0},"evidence_refs":[]}}
                """);
        PythonDashboardAiClient client = clientForServer();

        PythonDashboardAiClient.DashboardAiException exception = assertThrows(
                PythonDashboardAiClient.DashboardAiException.class,
                () -> client.matchTargetJob(Map.of("request_id", "rid"))
        );

        assertEquals("暂无可用岗位匹配结果", exception.getUserMessage());
    }

    @Test
    void mapsNoContentStatusToBusinessMessage() throws Exception {
        startServer(204, "");
        PythonDashboardAiClient client = clientForServer();

        PythonDashboardAiClient.DashboardAiException exception = assertThrows(
                PythonDashboardAiClient.DashboardAiException.class,
                () -> client.matchTargetJob(Map.of("request_id", "rid"))
        );

        assertEquals("暂无可用岗位匹配结果", exception.getUserMessage());
    }

    @Test
    void mapsValidationStatusToBusinessMessage() throws Exception {
        startServer(422, "{\"message\":\"missing fields\"}");
        PythonDashboardAiClient client = clientForServer();

        PythonDashboardAiClient.DashboardAiException exception = assertThrows(
                PythonDashboardAiClient.DashboardAiException.class,
                () -> client.matchTargetJob(Map.of("request_id", "rid"))
        );

        assertEquals("Dashboard-AI 请求参数错误", exception.getUserMessage());
    }

    @Test
    void mapsBadRequestValidationStatusToBusinessMessage() throws Exception {
        startServer(400, "{\"code\":0,\"msg\":\"VALIDATION_ERROR\",\"data\":{\"error\":\"missing fields\"}}");
        PythonDashboardAiClient client = clientForServer();

        PythonDashboardAiClient.DashboardAiException exception = assertThrows(
                PythonDashboardAiClient.DashboardAiException.class,
                () -> client.matchTargetJob(Map.of("request_id", "rid"))
        );

        assertEquals("Dashboard-AI 请求参数错误", exception.getUserMessage());
    }

    @Test
    void mapsServerErrorToUnavailableMessage() throws Exception {
        startServer(500, "{\"message\":\"boom\"}");
        PythonDashboardAiClient client = clientForServer();

        PythonDashboardAiClient.DashboardAiException exception = assertThrows(
                PythonDashboardAiClient.DashboardAiException.class,
                () -> client.matchTargetJob(Map.of("request_id", "rid"))
        );

        assertEquals("Dashboard-AI 服务暂不可用", exception.getUserMessage());
    }

    @Test
    void mapsNonJsonResponseToUnavailableMessage() throws Exception {
        startServer(200, "not json");
        PythonDashboardAiClient client = clientForServer();

        PythonDashboardAiClient.DashboardAiException exception = assertThrows(
                PythonDashboardAiClient.DashboardAiException.class,
                () -> client.matchTargetJob(Map.of("request_id", "rid"))
        );

        assertEquals("Dashboard-AI 服务暂不可用", exception.getUserMessage());
    }

    @Test
    void mapsTimeoutToTimeoutMessage() throws Exception {
        startServer(200, """
                {"code":1,"msg":"success","data":{"matched_job":{"job_id":101,"job_name":"AI算法工程师","score":0.87},"retrieval":{"fusion_method":"rrf"}}}
                """, 1500);
        PythonAiProperties properties = new PythonAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setDashboardTimeoutSeconds(1);
        PythonDashboardAiClient client = new PythonDashboardAiClient(new ObjectMapper(), properties);

        PythonDashboardAiClient.DashboardAiException exception = assertThrows(
                PythonDashboardAiClient.DashboardAiException.class,
                () -> client.matchTargetJob(Map.of("request_id", "rid"))
        );

        assertEquals("Dashboard-AI 服务超时，请稍后重试", exception.getUserMessage());
    }

    @Test
    void mapsConnectionFailureToUnavailableMessage() {
        PythonAiProperties properties = new PythonAiProperties();
        properties.setBaseUrl("http://127.0.0.1:1");
        PythonDashboardAiClient client = new PythonDashboardAiClient(new ObjectMapper(), properties);

        PythonDashboardAiClient.DashboardAiException exception = assertThrows(
                PythonDashboardAiClient.DashboardAiException.class,
                () -> client.matchTargetJob(Map.of("request_id", "rid"))
        );

        assertEquals("Dashboard-AI 服务暂不可用", exception.getUserMessage());
    }

    private PythonDashboardAiClient clientForServer() {
        PythonAiProperties properties = new PythonAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        return new PythonDashboardAiClient(new ObjectMapper(), properties);
    }

    private void startServer(int statusCode, String body) throws IOException {
        startServer(statusCode, body, 0);
    }

    private void startServer(int statusCode, String body, long delayMillis) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/dashboard/target-job/match", exchange -> {
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
