package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.vo.MarketInsightContentVO;
import com.itsheng.pojo.vo.MarketInsightVO;
import com.itsheng.pojo.vo.MarketJobDetailVO;
import com.itsheng.service.client.PythonMarketAiClient;
import com.itsheng.service.mapper.JobCategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private JobCategoryMapper jobCategoryMapper;

    @Mock
    private PythonMarketAiClient pythonMarketAiClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void getJobDetailUsesPythonSoftSkills() throws Exception {
        when(jobCategoryMapper.selectById(7L)).thenReturn(job());
        when(pythonMarketAiClient.generateSoftSkills(any())).thenReturn(objectMapper.readTree("""
                [
                  {
                    "name": "Communication",
                    "score": 82,
                    "description": "Clear stakeholder communication",
                    "evidence": ["Python-generated evidence"]
                  }
                ]
                """));

        MarketJobDetailVO detail = service().getJobDetail(7L);

        assertNotNull(detail);
        assertEquals(1, detail.getSoftSkills().size());
        assertEquals("Communication", detail.getSoftSkills().get(0).getName());
        assertEquals("Python-generated evidence", detail.getSoftSkills().get(0).getEvidence().get(0));
    }

    @Test
    void getJobDetailReturnsEmptySoftSkillsWhenPythonFails() {
        when(jobCategoryMapper.selectById(7L)).thenReturn(job());
        when(pythonMarketAiClient.generateSoftSkills(any())).thenThrow(new RuntimeException("down"));

        MarketJobDetailVO detail = service().getJobDetail(7L);

        assertNotNull(detail);
        assertTrue(detail.getSoftSkills().isEmpty());
    }

    @Test
    void getInsightReturnsEmptyStateAndDoesNotCacheWhenPythonFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(jobCategoryMapper.selectById(7L)).thenReturn(job());
        when(pythonMarketAiClient.generateMarketInsight(any())).thenThrow(new RuntimeException("down"));

        MarketInsightVO insight = service().getInsight(7L, "Beijing");

        MarketInsightContentVO content = insight.getInsight();
        assertNotNull(content);
        assertEquals("Market insight unavailable", content.getTitle());
        assertEquals("", content.getSummary());
        assertTrue(content.getMarketSignals().isEmpty());
        assertTrue(content.getIndustryTrends().isEmpty());
        assertTrue(content.getSuggestedActions().isEmpty());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    private MarketServiceImpl service() {
        return new MarketServiceImpl(jobCategoryMapper, objectMapper, pythonMarketAiClient, redisTemplate);
    }

    private JobCategory job() {
        return JobCategory.builder()
                .id(7L)
                .jobCategoryCode("AI_APP_JUNIOR")
                .jobCategoryName("AI Application Engineer")
                .jobLevel("JUNIOR")
                .jobLevelName("Junior")
                .minSalary(new BigDecimal("15000"))
                .maxSalary(new BigDecimal("25000"))
                .salaryUnit("MONTH")
                .sourceJobCount(120)
                .requiredExperienceYears(1)
                .requiredSkills("[\"Python\",\"RAG\"]")
                .jobDescription("Build AI/RAG applications")
                .build();
    }
}
