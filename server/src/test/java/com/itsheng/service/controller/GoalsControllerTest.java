package com.itsheng.service.controller;

import com.itsheng.common.exception.BaseException;
import com.itsheng.common.result.Result;
import com.itsheng.pojo.vo.AiAdviceVO;
import com.itsheng.service.service.GoalsService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoalsControllerTest {

    @Test
    void generateAiAdviceReturnsResultWithEvidenceAndDiagnostics() {
        GoalsService goalsService = mock(GoalsService.class);
        GoalsController controller = new GoalsController(goalsService);
        AiAdviceVO advice = AiAdviceVO.builder()
                .content("建议补齐 RAG 项目证据")
                .evidenceReferences(List.of(Map.of("sourceType", "milestone", "sourceId", "goal_7:chunk:0", "reason", "项目证据")))
                .retrievalDiagnostics(Map.of("retrieval", "multi_query+bm25+embedding", "fusion", "rag_fusion_rrf"))
                .build();
        when(goalsService.generateAiAdvice(7L)).thenReturn(advice);

        Result<AiAdviceVO> result = controller.generateAiAdvice(7L);

        assertEquals(1, result.getCode());
        assertEquals("建议补齐 RAG 项目证据", result.getData().getContent());
        assertEquals("milestone", result.getData().getEvidenceReferences().get(0).get("sourceType"));
        assertEquals("rag_fusion_rrf", result.getData().getRetrievalDiagnostics().get("fusion"));
    }

    @Test
    void generateAiAdviceMapsBusinessExceptionToResultError() {
        GoalsService goalsService = mock(GoalsService.class);
        GoalsController controller = new GoalsController(goalsService);
        when(goalsService.generateAiAdvice(7L)).thenThrow(new BaseException("目标不存在"));

        Result<AiAdviceVO> result = controller.generateAiAdvice(7L);

        assertEquals(0, result.getCode());
        assertEquals("目标不存在", result.getMsg());
    }

    @Test
    void updateMilestonePassesGoalIdAndMilestoneIdToService() {
        GoalsService goalsService = mock(GoalsService.class);
        GoalsController controller = new GoalsController(goalsService);

        Result<Void> result = controller.updateMilestone(7L, 11L, new com.itsheng.pojo.dto.GoalMilestoneUpdateDTO());

        assertEquals(1, result.getCode());
        verify(goalsService).updateMilestone(eq(7L), eq(11L), any());
    }
}
