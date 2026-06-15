package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.common.exception.BaseException;
import com.itsheng.pojo.dto.ResumeParsedData;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.vo.CareerPathRecommendationVO;
import com.itsheng.pojo.vo.UserTransitionRecommendationVO;
import com.itsheng.service.client.PythonRoadmapRagClient;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.UserProfileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoadmapServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearContext() {
        BaseContext.removeUserId();
    }

    @Test
    void buildRoadmapRagPayloadUsesBaseContextAndResumeWhitelist() {
        BaseContext.setUserId(10001L);
        RoadmapServiceImpl service = serviceWith(response("{\"lateralPaths\":[],\"diagnostics\":{}}"));
        ResumeParsedData resume = ResumeParsedData.builder()
                .name("Sensitive Name")
                .targetRole("AI Application Engineer")
                .currentRole("Frontend Engineer")
                .experienceYears(1)
                .skills(List.of("Vue", "Python"))
                .matchScore(78)
                .education(List.of(ResumeParsedData.Education.builder().school("Sensitive University").build()))
                .experience(List.of(ResumeParsedData.Experience.builder()
                        .company("Sensitive Company")
                        .description("private project description")
                        .build()))
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                service, "buildRoadmapRagPayload", "Frontend Engineer", List.of("Vue"), resume, jobs()
        );

        assertNotNull(payload);
        assertEquals(10001L, payload.get("userId"));
        String serialized = String.valueOf(payload);
        assertTrue(serialized.contains("AI Application Engineer"));
        assertFalse(serialized.contains("Sensitive Name"));
        assertFalse(serialized.contains("Sensitive University"));
        assertFalse(serialized.contains("Sensitive Company"));
        assertFalse(serialized.contains("private project description"));
    }

    @Test
    void generateLateralPathRecommendationsRagMapsPythonSuccess() {
        String evidenceCredential = "sk-" + "evidence-1234567890";
        String evidenceBareCredential = "sk-" + "evidencebare-1234567890";
        String diagnosticCredential = "sk-" + "diag-1234567890";
        String diagnosticBareCredential = "sk-" + "diagbare-1234567890";
        String diagnosticCredentialKey = "to" + "ken" + "=";
        String diagnosticCredentialValue = "diag" + "Credential123456789";
        RoadmapServiceImpl service = serviceWith(response("""
                {
                  "lateralPaths": [
                    {
                      "targetJobId": 2,
                      "targetCategoryCode": "AI_APP",
                      "targetJobName": "AI Application Engineer",
                      "matchScore": 0.86,
                      "transitionDifficulty": 3,
                      "estimatedMonths": 15,
                      "requiredSkills": ["RAG"],
                      "possessedSkills": ["Python"],
                      "aiRecommendationReason": "evidence based",
                      "evidence": [{
                        "documentType":"job",
                        "jobId":2,
                        "chunkId":"job:2:summary",
                        "score":0.03,
                        "source":"summary_index",
                        "rawText":"private resume 13812345678",
                        "prompt":"leak prompt",
                        "credentialMarker":"%s",
                        "sourceCredential":"%s"
                      }]
                    }
                  ],
                  "diagnostics": {
                    "queries":["Frontend Engineer %s%s","%s"],
                    "filters":{"excludeSameCategory":true,"documentTypes":["job"]},
                    "fusion":"rrf",
                    "reranker":"deterministic-fallback",
                    "candidateCount":2,
                    "rawText":"private resume 13812345678",
                    "credentialMarker":"%s"
                  }
                }
                """.formatted(
                evidenceCredential,
                evidenceBareCredential,
                diagnosticCredentialKey,
                diagnosticCredentialValue,
                diagnosticBareCredential,
                diagnosticCredential
        )));

        Object result = ReflectionTestUtils.invokeMethod(
                service, "generateLateralPathRecommendationsRAG", "Frontend Engineer", List.of("Python"), null, jobs()
        );

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<CareerPathRecommendationVO.LateralPathRecommendationVO> lateralPaths =
                (List<CareerPathRecommendationVO.LateralPathRecommendationVO>) ReflectionTestUtils.invokeMethod(result, "lateralPaths");
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) ReflectionTestUtils.invokeMethod(result, "diagnostics");
        assertEquals(1, lateralPaths.size());
        assertEquals("AI_APP", lateralPaths.get(0).getTargetCategoryCode());
        assertEquals(1, lateralPaths.get(0).getEvidence().size());
        Map<String, Object> evidence = lateralPaths.get(0).getEvidence().get(0);
        assertEquals("job", evidence.get("documentType"));
        assertEquals(2L, evidence.get("jobId"));
        assertFalse(evidence.containsKey("rawText"));
        assertFalse(evidence.containsKey("prompt"));
        assertFalse(evidence.containsKey("credentialMarker"));
        assertEquals("rrf", diagnostics.get("fusion"));
        assertEquals("deterministic-fallback", diagnostics.get("reranker"));
        assertEquals(2, diagnostics.get("candidateCount"));
        assertFalse(diagnostics.containsKey("rawText"));
        assertFalse(diagnostics.containsKey("credentialMarker"));
        assertFalse(String.valueOf(diagnostics).contains(diagnosticCredentialValue));
        assertFalse(String.valueOf(diagnostics).contains(diagnosticBareCredential));
        assertFalse(String.valueOf(lateralPaths).contains(evidenceCredential));
        assertFalse(String.valueOf(lateralPaths).contains(evidenceBareCredential));
    }

    @Test
    void generateLateralPathRecommendationsRagKeepsPythonEmptyRecommendations() {
        RoadmapServiceImpl service = serviceWith(response("{\"lateralPaths\":[],\"diagnostics\":{\"fusion\":\"rrf\"}}"));

        Object result = ReflectionTestUtils.invokeMethod(
                service, "generateLateralPathRecommendationsRAG", "Frontend Engineer", List.of("Python"), null, jobs()
        );

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<CareerPathRecommendationVO.LateralPathRecommendationVO> lateralPaths =
                (List<CareerPathRecommendationVO.LateralPathRecommendationVO>) ReflectionTestUtils.invokeMethod(result, "lateralPaths");
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) ReflectionTestUtils.invokeMethod(result, "diagnostics");
        assertTrue(lateralPaths.isEmpty());
        assertEquals("rrf", diagnostics.get("fusion"));
    }

    @Test
    void generateLateralPathRecommendationsRagDoesNotSupplementSinglePythonRecommendation() {
        RoadmapServiceImpl service = serviceWith(response("""
                {
                  "lateralPaths": [
                    {
                      "targetJobId": 2,
                      "targetCategoryCode": "AI_APP",
                      "targetJobName": "AI Application Engineer",
                      "matchScore": 0.86,
                      "transitionDifficulty": 3,
                      "estimatedMonths": 15,
                      "requiredSkills": ["RAG"],
                      "possessedSkills": ["Python"],
                      "aiRecommendationReason": "evidence based",
                      "evidence": []
                    }
                  ],
                  "diagnostics": {"fusion":"rrf","candidateCount":2}
                }
                """));

        Object result = ReflectionTestUtils.invokeMethod(
                service, "generateLateralPathRecommendationsRAG", "Frontend Engineer", List.of("Python"), null, jobs()
        );

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<CareerPathRecommendationVO.LateralPathRecommendationVO> lateralPaths =
                (List<CareerPathRecommendationVO.LateralPathRecommendationVO>) ReflectionTestUtils.invokeMethod(result, "lateralPaths");
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) ReflectionTestUtils.invokeMethod(result, "diagnostics");
        assertEquals(1, lateralPaths.size());
        assertFalse(diagnostics.containsKey("supplementedBy"));
        assertTrue(lateralPaths.stream().anyMatch(item -> "AI_APP".equals(item.getTargetCategoryCode())));
    }

    @Test
    void generateLateralPathRecommendationsRagFallsBackOnClientFailure() {
        PythonRoadmapRagClient client = mock(PythonRoadmapRagClient.class);
        when(client.generatePersonalizedRecommendations(any())).thenThrow(new RuntimeException("down"));
        RoadmapServiceImpl service = new RoadmapServiceImpl(
                mock(JobCategoryMapper.class),
                objectMapper,
                mock(UserProfileMapper.class),
                client,
                mock(StringRedisTemplate.class),
                mock(CacheManager.class)
        );

        Object result = ReflectionTestUtils.invokeMethod(
                service, "generateLateralPathRecommendationsRAG", "Frontend Engineer", List.of("Python"), null, jobs()
        );

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) ReflectionTestUtils.invokeMethod(result, "diagnostics");
        assertEquals("PYTHON_UNAVAILABLE", diagnostics.get("status"));
        assertEquals("none", diagnostics.get("fusion"));
    }

    @Test
    void pythonFailureDoesNotGenerateJavaRecommendationsAndRedactsReason() {
        String credential = "sk-" + "fallback-1234567890";
        PythonRoadmapRagClient client = mock(PythonRoadmapRagClient.class);
        when(client.generatePersonalizedRecommendations(any())).thenThrow(new RuntimeException("down"));
        RoadmapServiceImpl service = new RoadmapServiceImpl(
                mock(JobCategoryMapper.class),
                objectMapper,
                mock(UserProfileMapper.class),
                client,
                mock(StringRedisTemplate.class),
                mock(CacheManager.class)
        );

        Object result = ReflectionTestUtils.invokeMethod(
                service,
                "generateLateralPathRecommendationsRAG",
                "Frontend Engineer",
                List.of("Python", "18812345678", "user@example.com", "api" + "_key=" + credential),
                null,
                jobs()
        );

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<CareerPathRecommendationVO.LateralPathRecommendationVO> lateralPaths =
                (List<CareerPathRecommendationVO.LateralPathRecommendationVO>) ReflectionTestUtils.invokeMethod(result, "lateralPaths");
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) ReflectionTestUtils.invokeMethod(result, "diagnostics");
        assertTrue(lateralPaths.isEmpty());
        String serialized = String.valueOf(diagnostics);
        assertFalse(serialized.contains("18812345678"));
        assertFalse(serialized.contains("user@example.com"));
        assertFalse(serialized.contains(credential));
        assertEquals("PYTHON_UNAVAILABLE", diagnostics.get("status"));
    }

    @Test
    void generateLateralPathRecommendationsRagFallsBackOnInvalidFieldTypesAndRedactsFallbackQueries() {
        String currentCredential = "sk-" + "current-1234567890";
        RoadmapServiceImpl service = serviceWith(response("""
                {
                  "lateralPaths": [
                    {
                      "targetJobId": 2,
                      "targetCategoryCode": "AI_APP",
                      "targetJobName": "AI Application Engineer",
                      "matchScore": "not-a-number",
                      "transitionDifficulty": "hard",
                      "estimatedMonths": "soon",
                      "requiredSkills": "RAG",
                      "possessedSkills": ["Python"],
                      "evidence": []
                    }
                  ],
                  "diagnostics": {"fusion":"rrf","candidateCount":2}
                }
                """));

        Object result = ReflectionTestUtils.invokeMethod(
                service,
                "generateLateralPathRecommendationsRAG",
                "Frontend Engineer " + "api" + "_key=" + currentCredential,
                List.of("Python"),
                null,
                jobs()
        );

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnostics = (Map<String, Object>) ReflectionTestUtils.invokeMethod(result, "diagnostics");
        assertEquals("PYTHON_UNAVAILABLE", diagnostics.get("status"));
        assertEquals("none", diagnostics.get("fusion"));
        assertFalse(String.valueOf(diagnostics).contains(currentCredential));
    }

    @Test
    void parseRoadmapRagResultRejectsSchemaErrors() {
        RoadmapServiceImpl service = serviceWith(response("{\"lateralPaths\":[],\"diagnostics\":{}}"));
        JsonNode root = response("{\"lateralPaths\":{},\"diagnostics\":{}}");

        assertThrows(IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "parseRoadmapRagResult", root, jobs()));
    }

    @Test
    void recommendTransitionByJobNameUsesPythonRoadmapRag() {
        JobCategoryMapper jobCategoryMapper = mock(JobCategoryMapper.class);
        PythonRoadmapRagClient client = mock(PythonRoadmapRagClient.class);
        when(jobCategoryMapper.searchByKeyword("Frontend Engineer", 20)).thenReturn(List.of(jobs().get(0)));
        when(jobCategoryMapper.selectAll()).thenReturn(jobs());
        when(client.generatePersonalizedRecommendations(any())).thenReturn(response("""
                {
                  "lateralPaths": [
                    {
                      "targetJobId": 2,
                      "targetCategoryCode": "AI_APP",
                      "targetJobName": "AI Application Engineer",
                      "matchScore": 0.86,
                      "transitionDifficulty": 3,
                      "estimatedMonths": 15,
                      "requiredSkills": ["RAG"],
                      "possessedSkills": ["Python"],
                      "aiRecommendationReason": "evidence based",
                      "evidence": []
                    }
                  ],
                  "diagnostics": {"fusion":"rrf","candidateCount":3}
                }
                """));
        RoadmapServiceImpl service = new RoadmapServiceImpl(
                jobCategoryMapper,
                objectMapper,
                mock(UserProfileMapper.class),
                client,
                mock(StringRedisTemplate.class),
                mock(CacheManager.class)
        );

        UserTransitionRecommendationVO result =
                service.recommendTransitionByJobNameAndLevel("Frontend Engineer", "junior");

        assertEquals(List.of("Vue", "TypeScript"), result.getCurrentSkills());
        assertEquals(1, result.getRecommendations().size());
        UserTransitionRecommendationVO.TransitionRecommendationItemVO recommendation =
                result.getRecommendations().get(0);
        assertEquals(2L, recommendation.getToJobId());
        assertEquals(0.86, recommendation.getMatchScore());
        assertEquals(15, recommendation.getAvgTransitionTimeMonths());
        assertEquals("RAG", recommendation.getRequiredSkillsGap().get(0).getSkill());
        verify(client).generatePersonalizedRecommendations(any());
    }

    @Test
    void saveUserCurrentJobEvictsPersonalizedRecommendationCache() {
        BaseContext.setUserId(10001L);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(cacheManager.getCache("roadmap:recommendations:personalized")).thenReturn(cache);
        RoadmapServiceImpl service = new RoadmapServiceImpl(
                mock(JobCategoryMapper.class),
                objectMapper,
                mock(UserProfileMapper.class),
                mock(PythonRoadmapRagClient.class),
                redisTemplate,
                cacheManager
        );

        service.saveUserCurrentJob("AI Application Engineer");

        verify(valueOperations).set("roadmap:user:current_job:10001", "AI Application Engineer");
        verify(cache).evict(10001L);
    }

    @Test
    void saveUserCurrentJobThrowsWhenRedisSetFails() {
        BaseContext.setUserId(10001L);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(cacheManager.getCache("roadmap:recommendations:personalized")).thenReturn(cache);
        doThrow(new RuntimeException("redis down"))
                .when(valueOperations).set("roadmap:user:current_job:10001", "AI Application Engineer");
        RoadmapServiceImpl service = new RoadmapServiceImpl(
                mock(JobCategoryMapper.class),
                objectMapper,
                mock(UserProfileMapper.class),
                mock(PythonRoadmapRagClient.class),
                redisTemplate,
                cacheManager
        );

        BaseException exception = assertThrows(BaseException.class,
                () -> service.saveUserCurrentJob("AI Application Engineer"));

        assertEquals("保存当前岗位失败，请稍后重试", exception.getMessage());
        verify(cache, never()).evict(10001L);
    }

    @Test
    void roadmapServiceDoesNotUseSpringAiChatClient() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/itsheng/service/service/Impl/RoadmapServiceImpl.java"));

        assertFalse(source.contains("ChatClient"));
        assertFalse(source.contains(".prompt("));
        assertFalse(source.contains("systemPrompt"));
        assertFalse(source.contains("userPrompt"));
    }

    @Test
    void roadmapServiceLogsCurrentJobOnlyAsPresenceAndLength() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/itsheng/service/service/Impl/RoadmapServiceImpl.java"));

        assertFalse(source.contains("与 {} 的相似度"));
        assertTrue(source.contains("currentJobPresent"));
        assertTrue(source.contains("currentJobLength"));
    }

    private RoadmapServiceImpl serviceWith(JsonNode response) {
        PythonRoadmapRagClient client = mock(PythonRoadmapRagClient.class);
        when(client.generatePersonalizedRecommendations(any())).thenReturn(response);
        return new RoadmapServiceImpl(
                mock(JobCategoryMapper.class),
                objectMapper,
                mock(UserProfileMapper.class),
                client,
                mock(StringRedisTemplate.class),
                mock(CacheManager.class)
        );
    }

    private JsonNode response(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private List<JobCategory> jobs() {
        return List.of(
                JobCategory.builder()
                        .id(1L)
                        .jobCategoryCode("FRONTEND_JUNIOR")
                        .jobCategoryName("Frontend Engineer")
                        .jobLevel("JUNIOR")
                        .jobLevelName("Junior")
                        .requiredSkills("[\"Vue\",\"TypeScript\"]")
                        .minSalary(BigDecimal.TEN)
                        .maxSalary(BigDecimal.valueOf(15))
                        .salaryUnit("K/month")
                        .build(),
                JobCategory.builder()
                        .id(2L)
                        .jobCategoryCode("AI_APP_JUNIOR")
                        .jobCategoryName("AI Application Engineer")
                        .jobLevel("JUNIOR")
                        .jobLevelName("Junior")
                        .requiredSkills("[\"Python\",\"RAG\"]")
                        .jobDescription("Build AI and RAG applications")
                        .minSalary(BigDecimal.valueOf(12))
                        .maxSalary(BigDecimal.valueOf(18))
                        .salaryUnit("K/month")
                        .build(),
                JobCategory.builder()
                        .id(3L)
                        .jobCategoryCode("DATA_JUNIOR")
                        .jobCategoryName("Data Engineer")
                        .jobLevel("JUNIOR")
                        .jobLevelName("Junior")
                        .requiredSkills("[\"Python\",\"SQL\"]")
                        .minSalary(BigDecimal.valueOf(11))
                        .maxSalary(BigDecimal.valueOf(16))
                        .salaryUnit("K/month")
                        .build()
        );
    }
}
