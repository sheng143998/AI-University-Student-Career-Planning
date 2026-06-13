package com.itsheng.service.controller;

import com.itsheng.common.context.BaseContext;
import com.itsheng.common.result.Result;
import com.itsheng.service.client.PythonDashboardAiClient;
import com.itsheng.service.service.DashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    @AfterEach
    void tearDown() {
        BaseContext.removeUserId();
    }

    @Test
    void getRoadmapMapsDashboardAiExceptionToResultError() {
        DashboardService dashboardService = mock(DashboardService.class);
        DashboardController controller = new DashboardController(dashboardService);
        BaseContext.setUserId(1L);
        when(dashboardService.getRoadmap(1L))
                .thenThrow(new PythonDashboardAiClient.DashboardAiException("Dashboard-AI 服务暂不可用"));

        Result<Map<String, Object>> result = controller.getRoadmap();

        assertEquals(0, result.getCode());
        assertEquals("Dashboard-AI 服务暂不可用", result.getMsg());
    }
}
