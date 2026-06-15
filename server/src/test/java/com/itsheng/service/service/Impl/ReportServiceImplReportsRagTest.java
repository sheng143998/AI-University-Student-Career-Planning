package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.result.Result;
import com.itsheng.pojo.entity.CareerReport;
import com.itsheng.pojo.entity.ResumeAnalysisResult;
import com.itsheng.pojo.entity.StudentCapabilityProfile;
import com.itsheng.pojo.entity.UserCareerData;
import com.itsheng.service.client.PythonReportsAiClient;
import com.itsheng.service.controller.CommonController;
import com.itsheng.service.mapper.CareerReportMapper;
import com.itsheng.service.mapper.GoalMapper;
import com.itsheng.service.mapper.ResumeMapper;
import com.itsheng.service.mapper.StudentCapabilityProfileMapper;
import com.itsheng.service.mapper.UserCareerDataMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportServiceImplReportsRagTest {

    private CareerReportMapper careerReportMapper;
    private StudentCapabilityProfileMapper capabilityProfileMapper;
    private UserCareerDataMapper userCareerDataMapper;
    private GoalMapper goalMapper;
    private ResumeMapper resumeMapper;
    private PythonReportsAiClient pythonReportsAiClient;
    private CommonController commonController;
    private ReportServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        careerReportMapper = mock(CareerReportMapper.class);
        capabilityProfileMapper = mock(StudentCapabilityProfileMapper.class);
        userCareerDataMapper = mock(UserCareerDataMapper.class);
        goalMapper = mock(GoalMapper.class);
        resumeMapper = mock(ResumeMapper.class);
        pythonReportsAiClient = mock(PythonReportsAiClient.class);
        commonController = mock(CommonController.class);
        objectMapper = new ObjectMapper();
        service = new ReportServiceImpl(
                careerReportMapper,
                capabilityProfileMapper,
                userCareerDataMapper,
                goalMapper,
                resumeMapper,
                pythonReportsAiClient,
                objectMapper,
                commonController
        );
        when(commonController.upload(any())).thenReturn(Result.success("oss://reports/test.pdf"));
    }

    @Test
    void asyncGenerationStoresPythonEvidenceAndDiagnostics() throws Exception {
        givenReportInputs();
        JsonNode evidence = objectMapper.readTree("[{\"id\":\"e1\",\"sourceType\":\"resume_analysis\"}]");
        JsonNode diagnostics = objectMapper.readTree("{\"status\":\"OK\",\"selectedEvidenceCount\":1}");
        when(pythonReportsAiClient.generateSupport(anyMap()))
                .thenReturn(new PythonReportsAiClient.ReportsSupportResult("OK", "python suggestion", evidence, diagnostics));

        service.asyncGenerateReportContent(10L, 20L);

        ArgumentCaptor<CareerReport> captor = ArgumentCaptor.forClass(CareerReport.class);
        verify(careerReportMapper, atLeastOnce()).update(captor.capture());
        CareerReport completed = captor.getAllValues().stream()
                .filter(report -> "COMPLETED".equals(report.getStatus()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> matchDetails = objectMapper.readValue(completed.getMatchDetails(), Map.class);
        assertEquals("python suggestion", completed.getAiSuggestions());
        assertEquals("OK", ((Map<?, ?>) matchDetails.get("rag_diagnostics")).get("status"));
        assertFalse(((List<?>) matchDetails.get("evidence_refs")).isEmpty());
    }

    @Test
    void asyncGenerationStoresEmptyAiSupportWhenPythonFails() throws Exception {
        givenReportInputs();
        when(pythonReportsAiClient.generateSupport(anyMap())).thenThrow(new RuntimeException("downstream"));

        service.asyncGenerateReportContent(10L, 20L);

        ArgumentCaptor<CareerReport> captor = ArgumentCaptor.forClass(CareerReport.class);
        verify(careerReportMapper, atLeastOnce()).update(captor.capture());
        CareerReport completed = captor.getAllValues().stream()
                .filter(report -> "COMPLETED".equals(report.getStatus()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> matchDetails = objectMapper.readValue(completed.getMatchDetails(), Map.class);
        Map<?, ?> diagnostics = (Map<?, ?>) matchDetails.get("rag_diagnostics");
        assertEquals("COMPLETED", completed.getStatus());
        assertEquals("", completed.getAiSuggestions());
        assertEquals("PYTHON_UNAVAILABLE", diagnostics.get("status"));
    }

    @Test
    void asyncGenerationFailsWhenCapabilityMissing() {
        when(capabilityProfileMapper.selectByUserId(20L)).thenReturn(null);

        service.asyncGenerateReportContent(10L, 20L);

        ArgumentCaptor<CareerReport> captor = ArgumentCaptor.forClass(CareerReport.class);
        verify(careerReportMapper).update(captor.capture());
        assertEquals("FAILED", captor.getValue().getStatus());
        verifyNoInteractions(pythonReportsAiClient);
    }

    @Test
    void asyncGenerationStoresEmptyAiSupportForEmptyRetrieval() throws Exception {
        givenReportInputs();
        JsonNode evidence = objectMapper.readTree("[]");
        JsonNode diagnostics = objectMapper.readTree("{\"status\":\"EMPTY_RETRIEVAL\",\"emptyRetrieval\":true}");
        when(pythonReportsAiClient.generateSupport(anyMap()))
                .thenReturn(new PythonReportsAiClient.ReportsSupportResult("EMPTY_RETRIEVAL", "", evidence, diagnostics));

        service.asyncGenerateReportContent(10L, 20L);

        ArgumentCaptor<CareerReport> captor = ArgumentCaptor.forClass(CareerReport.class);
        verify(careerReportMapper, atLeastOnce()).update(captor.capture());
        CareerReport completed = captor.getAllValues().stream()
                .filter(report -> "COMPLETED".equals(report.getStatus()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> matchDetails = objectMapper.readValue(completed.getMatchDetails(), Map.class);
        assertEquals("", completed.getAiSuggestions());
        assertEquals("EMPTY_RETRIEVAL", ((Map<?, ?>) matchDetails.get("rag_diagnostics")).get("status"));
    }

    private void givenReportInputs() {
        StudentCapabilityProfile capability = StudentCapabilityProfile.builder()
                .id(1L)
                .userId(20L)
                .overallScore(85)
                .completenessScore(90)
                .competitivenessScore(80)
                .capabilityScores("{\"java\":88,\"database\":82}")
                .professionalSkills("[\"Java\",\"Spring Boot\"]")
                .softSkills("{\"communication\":80}")
                .aiEvaluation("Backend foundation is solid")
                .build();
        UserCareerData careerData = UserCareerData.builder()
                .userId(20L)
                .targetJob("Java backend engineer")
                .jobProfile("{\"requirements\":[\"Spring Boot\"]}")
                .build();
        ResumeAnalysisResult resume = ResumeAnalysisResult.builder()
                .id(3L)
                .userId(20L)
                .suggestions("Use metrics in backend project bullets")
                .build();
        when(capabilityProfileMapper.selectByUserId(20L)).thenReturn(capability);
        when(userCareerDataMapper.selectByUserId(20L)).thenReturn(careerData);
        when(goalMapper.findParallelByUserId(20L)).thenReturn(List.of());
        when(resumeMapper.selectByUserId(20L, null, 1)).thenReturn(List.of(resume));
        when(careerReportMapper.selectById(10L)).thenReturn(CareerReport.builder().id(10L).reportNo("CR1").build());
    }
}
