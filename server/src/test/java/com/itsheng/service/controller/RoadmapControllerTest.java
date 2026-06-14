package com.itsheng.service.controller;

import com.itsheng.common.exception.BaseException;
import com.itsheng.common.exception.GlobalExceptionHandler;
import com.itsheng.service.service.RoadmapService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RoadmapControllerTest {

    @Test
    void saveUserCurrentJobMapsBaseExceptionToResultError() throws Exception {
        RoadmapService roadmapService = mock(RoadmapService.class);
        doThrow(new BaseException("保存当前岗位失败，请稍后重试"))
                .when(roadmapService).saveUserCurrentJob(eq("AI Application Engineer"));
        MockMvc mockMvc = standaloneSetup(new RoadmapController(roadmapService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/roadmap/user/current-job")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentJob\":\"AI Application Engineer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("保存当前岗位失败，请稍后重试"));

        verify(roadmapService).saveUserCurrentJob("AI Application Engineer");
    }

    @Test
    void saveUserCurrentJobReturnsSuccessOnlyWhenServiceSucceeds() throws Exception {
        RoadmapService roadmapService = mock(RoadmapService.class);
        MockMvc mockMvc = standaloneSetup(new RoadmapController(roadmapService)).build();

        mockMvc.perform(post("/api/roadmap/user/current-job")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentJob\":\"AI Application Engineer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(roadmapService).saveUserCurrentJob("AI Application Engineer");
    }
}
