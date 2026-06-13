package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.ResumeAnalysisResult;
import com.itsheng.pojo.entity.UserCareerData;
import com.itsheng.pojo.entity.UserRoadmapSteps;
import com.itsheng.pojo.entity.UserVectorStore;
import com.itsheng.service.client.PythonDashboardAiClient;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.ResumeMapper;
import com.itsheng.service.mapper.UserCareerDataMapper;
import com.itsheng.service.mapper.UserRoadmapStepsMapper;
import com.itsheng.service.mapper.UserVectorStoreMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserRoadmapStepsMapper userRoadmapStepsMapper;

    @Mock
    private UserCareerDataMapper userCareerDataMapper;

    @Mock
    private JobCategoryMapper jobCategoryMapper;

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private UserVectorStoreMapper userVectorStoreMapper;

    @Mock
    private PythonDashboardAiClient pythonDashboardAiClient;

    @Test
    void getRoadmapUsesExistingValidTargetWithoutCallingPython() {
        DashboardServiceImpl service = service();
        UserCareerData careerData = UserCareerData.builder()
                .userId(1L)
                .targetJobId(101L)
                .targetJob("AI算法工程师")
                .build();
        JobCategory target = job(101L, "AI算法工程师", "AI_ENGINEER_JUNIOR", "JUNIOR");
        when(userRoadmapStepsMapper.selectByUserId(1L)).thenReturn(null);
        when(userCareerDataMapper.selectByUserId(1L)).thenReturn(careerData);
        when(jobCategoryMapper.selectById(101L)).thenReturn(target);
        when(jobCategoryMapper.selectVerticalPathByCategoryCode("AI_ENGINEER")).thenReturn(List.of(target));

        Map<String, Object> result = service.getRoadmap(1L);

        assertEquals(101L, result.get("target_job_id"));
        assertEquals("AI算法工程师", result.get("target_job_name"));
        verify(pythonDashboardAiClient, never()).matchTargetJob(any());
        verify(userRoadmapStepsMapper).insert(any(UserRoadmapSteps.class));
    }

    @Test
    void getRoadmapCallsPythonWhenTargetJobMissingAndPersistsResult() {
        DashboardServiceImpl service = service();
        String parsedData = """
                {
                  "target_role": "  AI算法工程师  ",
                  "skills": ["Python", "", "RAG", "Python", "机器学习", "模型部署", "Spring", "PostgreSQL", "Redis", "Docker", "Kubernetes", "向量检索", "BM25", "RRF", "LLM"],
                  "experience_years": 1,
                  "phone": "13800000000",
                  "email": "student@example.com",
                  "name": "张三",
                  "location": "杭州",
                  "education": [{"school": "某大学"}],
                  "experience": [{"company": "某公司"}],
                  "projects": [{"name": "敏感项目"}],
                  "raw_text": "完整简历原文",
                  "unknown_nested": {"rawText": "不应透传"}
                }
                """;
        ResumeAnalysisResult resume = ResumeAnalysisResult.builder()
                .id(10L)
                .userId(1L)
                .vectorStoreId("resume_vec_10")
                .parsedData(parsedData)
                .build();
        UserVectorStore vectorStore = UserVectorStore.builder()
                .id("resume_vec_10")
                .userId(1L)
                .resumeContent("使用 Python 构建 RAG 检索系统，包含 BM25 和 RRF。")
                .build();
        JobCategory matched = job(101L, "AI算法工程师", "AI_ENGINEER_JUNIOR", "JUNIOR");
        JobCategory senior = job(102L, "高级AI算法工程师", "AI_ENGINEER_SENIOR", "SENIOR");
        when(userRoadmapStepsMapper.selectByUserId(1L)).thenReturn(null);
        when(userCareerDataMapper.selectByUserId(1L)).thenReturn(null);
        when(resumeMapper.selectByUserId(1L, null, 1)).thenReturn(List.of(resume));
        when(userVectorStoreMapper.selectByVectorStoreIdAndUserId("resume_vec_10", 1L)).thenReturn(vectorStore);
        when(jobCategoryMapper.selectAll()).thenReturn(List.of(matched, senior));
        when(pythonDashboardAiClient.matchTargetJob(any()))
                .thenReturn(new PythonDashboardAiClient.TargetJobMatchResult(101L, "AI算法工程师", 0.88, null));
        when(jobCategoryMapper.selectById(101L)).thenReturn(matched);
        when(jobCategoryMapper.selectVerticalPathByCategoryCode("AI_ENGINEER")).thenReturn(List.of(matched, senior));

        Map<String, Object> result = service.getRoadmap(1L);

        assertEquals(101L, result.get("target_job_id"));
        assertEquals("AI算法工程师", result.get("target_job_name"));
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonDashboardAiClient).matchTargetJob(payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("dashboard-target-job-1-10", payload.get("request_id"));
        assertEquals("resume_vec_10", payload.get("resume_vector_store_id"));
        assertTrue(payload.containsKey("job_candidates"));
        assertEquals("使用 Python 构建 RAG 检索系统，包含 BM25 和 RRF。", payload.get("resume_content"));
        Map<String, Object> resumeProfile = (Map<String, Object>) payload.get("resume_profile");
        assertEquals(Set.of("target_role", "skills", "experience_years"), resumeProfile.keySet());
        assertEquals("AI算法工程师", resumeProfile.get("target_role"));
        assertEquals(1, resumeProfile.get("experience_years"));
        assertEquals(
                List.of("Python", "RAG", "机器学习", "模型部署", "Spring", "PostgreSQL",
                        "Redis", "Docker", "Kubernetes", "向量检索", "BM25", "RRF"),
                resumeProfile.get("skills")
        );
        assertFalse(resumeProfile.containsKey("phone"));
        assertFalse(resumeProfile.containsKey("email"));
        assertFalse(resumeProfile.containsKey("name"));
        assertFalse(resumeProfile.containsKey("location"));
        assertFalse(resumeProfile.containsKey("education"));
        assertFalse(resumeProfile.containsKey("experience"));
        assertFalse(resumeProfile.containsKey("projects"));
        assertFalse(resumeProfile.containsKey("raw_text"));
        assertFalse(resumeProfile.containsKey("unknown_nested"));

        ArgumentCaptor<UserCareerData> careerCaptor = ArgumentCaptor.forClass(UserCareerData.class);
        verify(userCareerDataMapper).insert(careerCaptor.capture());
        assertEquals(101L, careerCaptor.getValue().getTargetJobId());
        assertEquals("AI算法工程师", careerCaptor.getValue().getTargetJob());
        assertNotNull(careerCaptor.getValue().getJobProfile());

        ArgumentCaptor<UserRoadmapSteps> roadmapCaptor = ArgumentCaptor.forClass(UserRoadmapSteps.class);
        verify(userRoadmapStepsMapper).insert(roadmapCaptor.capture());
        assertEquals(101L, roadmapCaptor.getValue().getJobProfileId());
        assertTrue(roadmapCaptor.getValue().getSteps().contains("AI算法工程师"));
    }

    @Test
    void getRoadmapDoesNotCreateWhenPythonReturnsMissingJobId() {
        DashboardServiceImpl service = service();
        ResumeAnalysisResult resume = ResumeAnalysisResult.builder()
                .id(10L)
                .userId(1L)
                .vectorStoreId("resume_vec_10")
                .parsedData("{}")
                .build();
        UserVectorStore vectorStore = UserVectorStore.builder()
                .id("resume_vec_10")
                .userId(1L)
                .resumeContent("Python RAG 项目")
                .build();
        JobCategory candidate = job(101L, "AI算法工程师", "AI_ENGINEER_JUNIOR", "JUNIOR");
        when(userRoadmapStepsMapper.selectByUserId(1L)).thenReturn(null);
        when(userCareerDataMapper.selectByUserId(1L)).thenReturn(null);
        when(resumeMapper.selectByUserId(1L, null, 1)).thenReturn(List.of(resume));
        when(userVectorStoreMapper.selectByVectorStoreIdAndUserId("resume_vec_10", 1L)).thenReturn(vectorStore);
        when(jobCategoryMapper.selectAll()).thenReturn(List.of(candidate));
        when(pythonDashboardAiClient.matchTargetJob(any()))
                .thenReturn(new PythonDashboardAiClient.TargetJobMatchResult(999L, "不存在岗位", 0.7, null));
        when(jobCategoryMapper.selectById(999L)).thenReturn(null);

        Map<String, Object> result = service.getRoadmap(1L);

        assertEquals(null, result);
        verify(userCareerDataMapper, never()).insert(any(UserCareerData.class));
        verify(userRoadmapStepsMapper, never()).insert(any(UserRoadmapSteps.class));
    }

    @Test
    void getRoadmapRejectsVectorStoreOwnedByOtherUser() {
        DashboardServiceImpl service = service();
        ResumeAnalysisResult resume = ResumeAnalysisResult.builder()
                .id(10L)
                .userId(1L)
                .vectorStoreId("resume_vec_10")
                .parsedData("{}")
                .build();
        UserVectorStore vectorStore = UserVectorStore.builder()
                .id("resume_vec_10")
                .userId(2L)
                .resumeContent("其他用户的 Python RAG 项目经历")
                .build();
        when(userRoadmapStepsMapper.selectByUserId(1L)).thenReturn(null);
        when(userCareerDataMapper.selectByUserId(1L)).thenReturn(null);
        when(resumeMapper.selectByUserId(1L, null, 1)).thenReturn(List.of(resume));
        when(userVectorStoreMapper.selectByVectorStoreIdAndUserId("resume_vec_10", 1L)).thenReturn(null);

        Map<String, Object> result = service.getRoadmap(1L);

        assertEquals(null, result);
        verify(pythonDashboardAiClient, never()).matchTargetJob(any());
        verify(userVectorStoreMapper, never()).selectByVectorStoreId("resume_vec_10");
        verify(userCareerDataMapper, never()).insert(any(UserCareerData.class));
        verify(userRoadmapStepsMapper, never()).insert(any(UserRoadmapSteps.class));
    }

    @Test
    void getRoadmapCallsPythonWhenExistingTargetJobInvalid() {
        DashboardServiceImpl service = service();
        UserCareerData careerData = UserCareerData.builder()
                .userId(1L)
                .targetJobId(404L)
                .targetJob("已删除岗位")
                .build();
        ResumeAnalysisResult resume = ResumeAnalysisResult.builder()
                .id(10L)
                .userId(1L)
                .vectorStoreId("resume_vec_10")
                .parsedData("{\"target_role\":\"AI算法工程师\",\"skills\":[\"Python\",\"RAG\"]}")
                .build();
        UserVectorStore vectorStore = UserVectorStore.builder()
                .id("resume_vec_10")
                .userId(1L)
                .resumeContent("使用 Python 构建 RAG 检索系统，包含 BM25 和 RRF。")
                .build();
        JobCategory matched = job(101L, "AI算法工程师", "AI_ENGINEER_JUNIOR", "JUNIOR");
        JobCategory senior = job(102L, "高级AI算法工程师", "AI_ENGINEER_SENIOR", "SENIOR");
        UserCareerData updatedCareerData = UserCareerData.builder()
                .userId(1L)
                .targetJobId(101L)
                .targetJob("AI算法工程师")
                .build();
        when(userRoadmapStepsMapper.selectByUserId(1L)).thenReturn(null);
        when(userCareerDataMapper.selectByUserId(1L)).thenReturn(careerData, updatedCareerData);
        when(jobCategoryMapper.selectById(404L)).thenReturn(null);
        when(resumeMapper.selectByUserId(1L, null, 1)).thenReturn(List.of(resume));
        when(userVectorStoreMapper.selectByVectorStoreIdAndUserId("resume_vec_10", 1L)).thenReturn(vectorStore);
        when(jobCategoryMapper.selectAll()).thenReturn(List.of(matched, senior));
        when(pythonDashboardAiClient.matchTargetJob(any()))
                .thenReturn(new PythonDashboardAiClient.TargetJobMatchResult(101L, "AI算法工程师", 0.88, null));
        when(jobCategoryMapper.selectById(101L)).thenReturn(matched);
        when(jobCategoryMapper.selectVerticalPathByCategoryCode("AI_ENGINEER")).thenReturn(List.of(matched, senior));

        Map<String, Object> result = service.getRoadmap(1L);

        assertEquals(101L, result.get("target_job_id"));
        verify(pythonDashboardAiClient).matchTargetJob(any());
        verify(userCareerDataMapper).update(any(UserCareerData.class));
        verify(userRoadmapStepsMapper).insert(any(UserRoadmapSteps.class));
    }

    @Test
    void getRoadmapWhitelistProfileAcceptsDelimitedSkillsAndDropsInvalidValues() {
        DashboardServiceImpl service = service();
        ResumeAnalysisResult resume = ResumeAnalysisResult.builder()
                .id(11L)
                .userId(1L)
                .vectorStoreId("resume_vec_11")
                .parsedData("""
                        {
                          "target_role": "   ",
                          "skills": " Python，RAG、Python,  ,BM25 ",
                          "experience_years": "1",
                          "rawText": "画像原文不应透传"
                        }
                        """)
                .build();
        UserVectorStore vectorStore = UserVectorStore.builder()
                .id("resume_vec_11")
                .userId(1L)
                .resumeContent("Python RAG 项目")
                .build();
        JobCategory matched = job(101L, "AI算法工程师", "AI_ENGINEER_JUNIOR", "JUNIOR");
        when(userRoadmapStepsMapper.selectByUserId(1L)).thenReturn(null);
        when(userCareerDataMapper.selectByUserId(1L)).thenReturn(null);
        when(resumeMapper.selectByUserId(1L, null, 1)).thenReturn(List.of(resume));
        when(userVectorStoreMapper.selectByVectorStoreIdAndUserId("resume_vec_11", 1L)).thenReturn(vectorStore);
        when(jobCategoryMapper.selectAll()).thenReturn(List.of(matched));
        when(pythonDashboardAiClient.matchTargetJob(any()))
                .thenReturn(new PythonDashboardAiClient.TargetJobMatchResult(101L, "AI算法工程师", 0.88, null));
        when(jobCategoryMapper.selectById(101L)).thenReturn(matched);
        when(jobCategoryMapper.selectVerticalPathByCategoryCode("AI_ENGINEER")).thenReturn(List.of(matched));

        service.getRoadmap(1L);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonDashboardAiClient).matchTargetJob(payloadCaptor.capture());
        Map<String, Object> resumeProfile = (Map<String, Object>) payloadCaptor.getValue().get("resume_profile");
        assertEquals(Set.of("skills"), resumeProfile.keySet());
        assertEquals(List.of("Python", "RAG", "BM25"), resumeProfile.get("skills"));
        assertFalse(resumeProfile.containsKey("target_role"));
        assertFalse(resumeProfile.containsKey("experience_years"));
        assertFalse(resumeProfile.containsKey("rawText"));
    }

    @Test
    void getRoadmapWhitelistProfileDropsNestedAllowedFieldValues() {
        DashboardServiceImpl service = service();
        ResumeAnalysisResult resume = ResumeAnalysisResult.builder()
                .id(12L)
                .userId(1L)
                .vectorStoreId("resume_vec_12")
                .parsedData("""
                        {
                          "target_role": {"text": "AI算法工程师"},
                          "skills": ["Python", {"name": "RAG"}, true, 123],
                          "experience_years": 2
                        }
                        """)
                .build();
        UserVectorStore vectorStore = UserVectorStore.builder()
                .id("resume_vec_12")
                .userId(1L)
                .resumeContent("Python RAG 项目")
                .build();
        JobCategory matched = job(101L, "AI算法工程师", "AI_ENGINEER_JUNIOR", "JUNIOR");
        when(userRoadmapStepsMapper.selectByUserId(1L)).thenReturn(null);
        when(userCareerDataMapper.selectByUserId(1L)).thenReturn(null);
        when(resumeMapper.selectByUserId(1L, null, 1)).thenReturn(List.of(resume));
        when(userVectorStoreMapper.selectByVectorStoreIdAndUserId("resume_vec_12", 1L)).thenReturn(vectorStore);
        when(jobCategoryMapper.selectAll()).thenReturn(List.of(matched));
        when(pythonDashboardAiClient.matchTargetJob(any()))
                .thenReturn(new PythonDashboardAiClient.TargetJobMatchResult(101L, "AI算法工程师", 0.88, null));
        when(jobCategoryMapper.selectById(101L)).thenReturn(matched);
        when(jobCategoryMapper.selectVerticalPathByCategoryCode("AI_ENGINEER")).thenReturn(List.of(matched));

        service.getRoadmap(1L);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonDashboardAiClient).matchTargetJob(payloadCaptor.capture());
        Map<String, Object> resumeProfile = (Map<String, Object>) payloadCaptor.getValue().get("resume_profile");
        assertEquals(Set.of("skills", "experience_years"), resumeProfile.keySet());
        assertEquals(List.of("Python", "true", "123"), resumeProfile.get("skills"));
        assertEquals(2, resumeProfile.get("experience_years"));
        assertFalse(resumeProfile.containsKey("target_role"));
    }

    @Test
    void getRoadmapWhitelistProfileDropsSensitiveAllowedFieldValues() {
        DashboardServiceImpl service = service();
        ResumeAnalysisResult resume = ResumeAnalysisResult.builder()
                .id(13L)
                .userId(1L)
                .vectorStoreId("resume_vec_13")
                .parsedData("""
                        {
                          "target_role": "student@example.com",
                          "skills": ["Python", "13800000000", "api_key=abcdef1234567890abcdef123456"],
                          "experience_years": 2
                        }
                        """)
                .build();
        UserVectorStore vectorStore = UserVectorStore.builder()
                .id("resume_vec_13")
                .userId(1L)
                .resumeContent("Python RAG 项目")
                .build();
        JobCategory matched = job(101L, "AI算法工程师", "AI_ENGINEER_JUNIOR", "JUNIOR");
        when(userRoadmapStepsMapper.selectByUserId(1L)).thenReturn(null);
        when(userCareerDataMapper.selectByUserId(1L)).thenReturn(null);
        when(resumeMapper.selectByUserId(1L, null, 1)).thenReturn(List.of(resume));
        when(userVectorStoreMapper.selectByVectorStoreIdAndUserId("resume_vec_13", 1L)).thenReturn(vectorStore);
        when(jobCategoryMapper.selectAll()).thenReturn(List.of(matched));
        when(pythonDashboardAiClient.matchTargetJob(any()))
                .thenReturn(new PythonDashboardAiClient.TargetJobMatchResult(101L, "AI算法工程师", 0.88, null));
        when(jobCategoryMapper.selectById(101L)).thenReturn(matched);
        when(jobCategoryMapper.selectVerticalPathByCategoryCode("AI_ENGINEER")).thenReturn(List.of(matched));

        service.getRoadmap(1L);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonDashboardAiClient).matchTargetJob(payloadCaptor.capture());
        Map<String, Object> resumeProfile = (Map<String, Object>) payloadCaptor.getValue().get("resume_profile");
        assertEquals(Set.of("skills", "experience_years"), resumeProfile.keySet());
        assertEquals(List.of("Python"), resumeProfile.get("skills"));
        assertEquals(2, resumeProfile.get("experience_years"));
        assertFalse(resumeProfile.containsKey("target_role"));
    }

    private DashboardServiceImpl service() {
        return new DashboardServiceImpl(
                userRoadmapStepsMapper,
                userCareerDataMapper,
                jobCategoryMapper,
                resumeMapper,
                userVectorStoreMapper,
                pythonDashboardAiClient,
                new ObjectMapper()
        );
    }

    private JobCategory job(Long id, String name, String code, String level) {
        return JobCategory.builder()
                .id(id)
                .jobCategoryName(name)
                .jobCategoryCode(code)
                .jobLevel(level)
                .jobLevelName(level)
                .requiredSkills("[\"Python\",\"RAG\"]")
                .jobDescription("负责 RAG 检索和模型应用落地。")
                .jobProfile("{\"industrySegment\":\"人工智能\"}")
                .build();
    }
}
