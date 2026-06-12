package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.pojo.entity.ResumeAnalysisResult;
import com.itsheng.pojo.entity.StudentCapabilityProfile;
import com.itsheng.pojo.entity.UserVectorStore;
import com.itsheng.service.client.PythonResumeAiClient;
import com.itsheng.service.controller.CommonController;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.ResumeMapper;
import com.itsheng.service.mapper.StudentCapabilityProfileMapper;
import com.itsheng.service.mapper.UserCareerDataMapper;
import com.itsheng.service.mapper.UserRoadmapStepsMapper;
import com.itsheng.service.mapper.UserVectorStoreMapper;
import com.itsheng.service.service.ResumeOcrService;
import com.itsheng.common.utils.AliOssUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplResumePythonTest {

    @Mock
    private CommonController commonController;
    @Mock
    private UserVectorStoreMapper userVectorStoreMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private StudentCapabilityProfileMapper capabilityProfileMapper;
    @Mock
    private AliOssUtil aliOssUtil;
    @Mock
    private PythonResumeAiClient pythonResumeAiClient;
    @Mock
    private JobCategoryMapper jobCategoryMapper;
    @Mock
    private UserCareerDataMapper userCareerDataMapper;
    @Mock
    private UserRoadmapStepsMapper userRoadmapStepsMapper;
    @Mock
    private ResumeOcrService resumeOcrService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearBaseContext() {
        BaseContext.removeUserId();
    }

    @Test
    void analyzeAndSaveCallsPythonAndPersistsReturnedFields() throws Exception {
        ResumeServiceImpl service = service();
        mockInsertGeneratedId(11L);
        doReturn(pythonResult()).when(pythonResumeAiClient).analyze(any());

        service.analyzeAndSave("vs-1", 1001L, "Name: Ada\nPython RAG project", "txt", "resume.txt", "oss://resume.txt");

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonResumeAiClient).analyze(payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("vs-1", payload.get("vector_store_id"));
        assertEquals(1001L, payload.get("user_id"));
        assertEquals("Name: Ada\nPython RAG project", payload.get("resume_text"));
        assertEquals("txt", payload.get("file_type"));
        assertEquals("resume.txt", payload.get("original_file_name"));
        assertEquals("oss://resume.txt", payload.get("resume_file_path"));

        ArgumentCaptor<ResumeAnalysisResult> resultCaptor = ArgumentCaptor.forClass(ResumeAnalysisResult.class);
        verify(resumeMapper).insert(any(ResumeAnalysisResult.class));
        verify(resumeMapper, org.mockito.Mockito.atLeastOnce()).updateByIdAndUserId(resultCaptor.capture());
        ResumeAnalysisResult completed = resultCaptor.getAllValues().stream()
                .filter(row -> "completed".equals(row.getStatus()))
                .findFirst()
                .orElseThrow();
        assertEquals("vs-1", completed.getVectorStoreId());
        assertEquals(1001L, completed.getUserId());
        assertEquals("completed", completed.getStatus());
        assertEquals(100, completed.getProgress());
        assertEquals("{\"target_role\":\"AI Agent Intern\",\"skills\":[\"Python\",\"RAG\"]}", completed.getParsedData());
        assertEquals("[\"RAG evidence\"]", completed.getHighlights());
        assertEquals("[{\"type\":\"SKILL\",\"content\":\"Add project metrics\"}]", completed.getSuggestions());

        ArgumentCaptor<StudentCapabilityProfile> profileCaptor = ArgumentCaptor.forClass(StudentCapabilityProfile.class);
        verify(capabilityProfileMapper).insert(profileCaptor.capture());
        StudentCapabilityProfile profile = profileCaptor.getValue();
        assertEquals(1001L, profile.getUserId());
        assertEquals(81, profile.getOverallScore());
        assertEquals("{\"professional_skill\":82}", profile.getCapabilityScores());

        ArgumentCaptor<UserVectorStore> storeCaptor = ArgumentCaptor.forClass(UserVectorStore.class);
        verify(userVectorStoreMapper).upsert(storeCaptor.capture());
        UserVectorStore store = storeCaptor.getValue();
        assertEquals("vs-1", store.getId());
        assertEquals(1001L, store.getUserId());
        assertEquals("resume", store.getVectorType());
        assertNotNull(store.getMetadata());
    }

    @Test
    void analyzeAndSaveWritesFailedStatusWhenPythonFails() {
        ResumeServiceImpl service = service();
        mockInsertGeneratedId(12L);
        doReturn(null).when(pythonResumeAiClient).analyze(any());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.analyzeAndSave("vs-fail", 1002L, "resume", "txt", "resume.txt", "oss://resume.txt"));

        ArgumentCaptor<ResumeAnalysisResult> resultCaptor = ArgumentCaptor.forClass(ResumeAnalysisResult.class);
        verify(resumeMapper, org.mockito.Mockito.atLeastOnce()).updateByIdAndUserId(resultCaptor.capture());
        ResumeAnalysisResult failed = resultCaptor.getAllValues().stream()
                .filter(row -> "failed".equals(row.getStatus()))
                .findFirst()
                .orElseThrow();
        assertEquals("vs-fail", failed.getVectorStoreId());
        assertEquals(1002L, failed.getUserId());
        assertEquals(0, failed.getProgress());
        verify(capabilityProfileMapper, never()).insert(any());
    }

    @Test
    void analyzeAndSaveReusesExistingAnalysisRowForIdempotency() throws Exception {
        ResumeServiceImpl service = service();
        doReturn(ResumeAnalysisResult.builder()
                .id(77L)
                .vectorStoreId("vs-existing")
                .userId(1004L)
                .createTime(java.time.LocalDateTime.now().minusDays(1))
                .build())
                .when(resumeMapper).selectByVectorStoreIdAndUserId("vs-existing", 1004L);
        doReturn(pythonResult()).when(pythonResumeAiClient).analyze(any());

        service.analyzeAndSave("vs-existing", 1004L, "Python RAG", "txt", "resume.txt", "oss://resume.txt");

        verify(resumeMapper, never()).insert(any());
        ArgumentCaptor<ResumeAnalysisResult> resultCaptor = ArgumentCaptor.forClass(ResumeAnalysisResult.class);
        verify(resumeMapper, org.mockito.Mockito.atLeastOnce()).updateByIdAndUserId(resultCaptor.capture());
        ResumeAnalysisResult completed = resultCaptor.getAllValues().stream()
                .filter(row -> "completed".equals(row.getStatus()))
                .findFirst()
                .orElseThrow();
        assertEquals(77L, completed.getId());
        assertEquals(1004L, completed.getUserId());
    }

    @Test
    void getAnalysisResultUsesCurrentUserBoundary() {
        ResumeServiceImpl service = service();
        BaseContext.setUserId(1003L);
        doReturn(UserVectorStore.builder()
                .id("vs-boundary")
                .userId(1003L)
                .resumeFilePath("oss://resume.txt")
                .build())
                .when(userVectorStoreMapper).selectByVectorStoreIdAndUserId("vs-boundary", 1003L);

        assertEquals(1003L, service.getAnalysisResult("vs-boundary").getUserId());
        verify(resumeMapper).selectByVectorStoreIdAndUserId("vs-boundary", 1003L);
        verify(userVectorStoreMapper).selectByVectorStoreIdAndUserId("vs-boundary", 1003L);
    }

    private ResumeServiceImpl service() {
        return new ResumeServiceImpl(
                commonController,
                userVectorStoreMapper,
                resumeMapper,
                capabilityProfileMapper,
                aliOssUtil,
                pythonResumeAiClient,
                objectMapper,
                jobCategoryMapper,
                userCareerDataMapper,
                userRoadmapStepsMapper,
                resumeOcrService
        );
    }

    private void mockInsertGeneratedId(Long id) {
        doAnswer(invocation -> {
            ResumeAnalysisResult row = invocation.getArgument(0);
            row.setId(id);
            return 1;
        }).when(resumeMapper).insert(any(ResumeAnalysisResult.class));
    }

    private com.fasterxml.jackson.databind.JsonNode pythonResult() throws Exception {
        return objectMapper.readTree("""
                {
                  "status": "completed",
                  "parsed_data": {"target_role": "AI Agent Intern", "skills": ["Python", "RAG"]},
                  "scores": {"keyword_match": 88, "layout": 76, "skill_depth": 82, "experience": 70},
                  "highlights": ["RAG evidence"],
                  "suggestions": [{"type": "SKILL", "content": "Add project metrics"}],
                  "capability_profile": {
                    "overall_score": 81,
                    "completeness_score": 77,
                    "competitiveness_score": 79,
                    "capability_scores": {"professional_skill": 82},
                    "professional_skills": [{"name": "Python"}],
                    "certificates": [],
                    "soft_skills": {"learning": {"score": 80}},
                    "ai_evaluation": "Deterministic fallback"
                  },
                  "rag_diagnostics": {
                    "retrieval": {"bm25": true, "embedding_fallback": "hash", "fusion": "rrf", "reranker": "deterministic"}
                  }
                }
                """);
    }
}
