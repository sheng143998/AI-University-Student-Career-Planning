package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.JobVectorStore;
import com.itsheng.service.client.PythonMarketAiClient;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.JobVectorStoreMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobVectorSearchServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private JobCategoryMapper jobCategoryMapper;

    @Mock
    private JobVectorStoreMapper jobVectorStoreMapper;

    @Mock
    private PythonMarketAiClient pythonMarketAiClient;

    @Test
    void searchSimilarJobsPreservesPythonRanking() {
        JobCategory frontend = job(1L, "Frontend Engineer", "FRONTEND_DEV_JUNIOR", "Vue");
        JobCategory ai = job(2L, "AI Application Engineer", "AI_APP_JUNIOR", "Python RAG");
        when(jobCategoryMapper.selectAll()).thenReturn(List.of(frontend, ai));
        when(pythonMarketAiClient.searchJobs(any())).thenReturn(new PythonMarketAiClient.JobSearchResult(
                List.of(2L, 1L),
                objectMapper.createArrayNode(),
                objectMapper.createObjectNode()
        ));
        JobVectorSearchServiceImpl service = service();

        List<JobCategory> result = service.searchSimilarJobs("Python RAG", 2);

        assertEquals(List.of(2L, 1L), result.stream().map(JobCategory::getId).toList());
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonMarketAiClient).searchJobs(payloadCaptor.capture());
        assertEquals("Python RAG", payloadCaptor.getValue().get("query_text"));
        assertTrue(payloadCaptor.getValue().containsKey("jobs"));
    }

    @Test
    void searchSimilarJobsFallsBackToKeywordSearchOnPythonFailure() {
        JobCategory ai = job(2L, "AI Application Engineer", "AI_APP_JUNIOR", "Python RAG");
        when(jobCategoryMapper.selectAll()).thenReturn(List.of(ai));
        when(pythonMarketAiClient.searchJobs(any())).thenThrow(new RuntimeException("down"));
        when(jobCategoryMapper.searchByKeyword("Python RAG", 3)).thenReturn(List.of(ai));
        JobVectorSearchServiceImpl service = service();

        List<JobCategory> result = service.searchSimilarJobs("Python RAG", 3);

        assertEquals(List.of(2L), result.stream().map(JobCategory::getId).toList());
    }

    @Test
    void getJobVectorReadsStoredPythonEmbedding() {
        when(jobVectorStoreMapper.selectByJobId(2L)).thenReturn(JobVectorStore.builder()
                .jobId(2L)
                .embeddingVector("[0.1,0.2]")
                .build());

        assertEquals("[0.1,0.2]", service().getJobVector(2L));
    }

    @Test
    void jobVectorSearchServiceDoesNotUseJavaEmbeddingOrPrompt() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/itsheng/service/service/Impl/JobVectorSearchServiceImpl.java"));

        assertFalse(source.contains("OpenAiEmbeddingModel"));
        assertFalse(source.contains("SimpleVectorStore"));
        assertFalse(source.contains(".prompt("));
        assertFalse(source.contains("ChatClient"));
    }

    private JobVectorSearchServiceImpl service() {
        return new JobVectorSearchServiceImpl(jobCategoryMapper, jobVectorStoreMapper, pythonMarketAiClient);
    }

    private JobCategory job(Long id, String name, String code, String skills) {
        return JobCategory.builder()
                .id(id)
                .jobCategoryName(name)
                .jobCategoryCode(code)
                .jobLevel("JUNIOR")
                .requiredSkills("[\"" + skills + "\"]")
                .jobDescription("Build " + name)
                .build();
    }
}
