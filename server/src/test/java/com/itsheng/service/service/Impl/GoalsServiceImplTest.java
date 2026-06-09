package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.common.exception.BaseException;
import com.itsheng.pojo.dto.GoalMilestoneUpdateDTO;
import com.itsheng.pojo.dto.GoalUpdateDTO;
import com.itsheng.pojo.entity.Goal;
import com.itsheng.pojo.entity.GoalMilestone;
import com.itsheng.pojo.vo.AiAdviceVO;
import com.itsheng.service.client.PythonGoalsAdviceClient;
import com.itsheng.service.mapper.GoalMapper;
import com.itsheng.service.mapper.GoalMilestoneMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoalsServiceImplTest {

    private final GoalMapper goalMapper = mock(GoalMapper.class);
    private final GoalMilestoneMapper milestoneMapper = mock(GoalMilestoneMapper.class);
    private final PythonGoalsAdviceClient pythonGoalsAdviceClient = mock(PythonGoalsAdviceClient.class);
    private final GoalsServiceImpl service = new GoalsServiceImpl(
            goalMapper,
            milestoneMapper,
            new ObjectMapper(),
            pythonGoalsAdviceClient
    );

    @AfterEach
    void tearDown() {
        BaseContext.removeUserId();
    }

    @Test
    void generateAiAdviceUsesUserScopedGoalAndMetadataFilters() {
        BaseContext.setUserId(10001L);
        when(goalMapper.findByIdAndUserId(7L, 10001L)).thenReturn(goal());
        when(milestoneMapper.findByGoalIdAndUserId(7L, 10001L)).thenReturn(List.of(milestone(10001L)));
        AiAdviceVO response = AiAdviceVO.builder()
                .content("建议补齐 RAG 项目证据")
                .evidenceReferences(List.of(Map.of("sourceType", "milestone", "sourceId", "goal_7:chunk:0", "reason", "项目证据")))
                .retrievalDiagnostics(Map.of("metadataFilters", Map.of("userId", "10001", "goalId", "7")))
                .build();
        when(pythonGoalsAdviceClient.generateGoalAdvice(any())).thenReturn(response);
        when(goalMapper.updateAiAdviceByIdAndUserId(7L, 10001L, "建议补齐 RAG 项目证据")).thenReturn(1);

        AiAdviceVO result = service.generateAiAdvice(7L);

        assertEquals("建议补齐 RAG 项目证据", result.getContent());
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonGoalsAdviceClient).generateGoalAdvice(payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        Map<?, ?> retrievalOptions = (Map<?, ?>) payload.get("retrievalOptions");
        Map<?, ?> filters = (Map<?, ?>) retrievalOptions.get("metadataFilters");
        assertEquals("10001", filters.get("userId"));
        assertEquals("7", filters.get("goalId"));
        List<?> milestones = (List<?>) payload.get("milestones");
        assertEquals(1, milestones.size());
        verify(goalMapper).updateAiAdviceByIdAndUserId(7L, 10001L, "建议补齐 RAG 项目证据");
    }

    @Test
    void generateAiAdviceDoesNotCallPythonWhenGoalIsMissingOrCrossUser() {
        BaseContext.setUserId(10001L);
        when(goalMapper.findByIdAndUserId(7L, 10001L)).thenReturn(null);

        BaseException exception = assertThrows(BaseException.class, () -> service.generateAiAdvice(7L));

        assertEquals("目标不存在", exception.getMessage());
        verifyNoInteractions(pythonGoalsAdviceClient);
    }

    @Test
    void generateAiAdviceSanitizesSensitivePayloadValues() {
        BaseContext.setUserId(10001L);
        Goal goal = goal();
        goal.setGoalDesc("联系 13812345678 或 test@example.com，token=abcdef123456，sk-abcdefghijkl，sk-proj-abcdefghijklmnop");
        when(goalMapper.findByIdAndUserId(7L, 10001L)).thenReturn(goal);
        when(milestoneMapper.findByGoalIdAndUserId(7L, 10001L)).thenReturn(List.of(milestone(10001L)));
        when(pythonGoalsAdviceClient.generateGoalAdvice(any())).thenReturn(new AiAdviceVO("安全建议"));
        when(goalMapper.updateAiAdviceByIdAndUserId(7L, 10001L, "安全建议")).thenReturn(1);

        service.generateAiAdvice(7L);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(pythonGoalsAdviceClient).generateGoalAdvice(payloadCaptor.capture());
        String serialized = payloadCaptor.getValue().toString();
        assertFalse(serialized.contains("13812345678"));
        assertFalse(serialized.contains("test@example.com"));
        assertFalse(serialized.contains("abcdef123456"));
        assertFalse(serialized.contains("sk-abcdefghijkl"));
        assertFalse(serialized.contains("sk-proj-abcdefghijklmnop"));
    }

    @Test
    void deleteGoalDoesNotDeleteMilestonesWhenGoalDoesNotBelongToUser() {
        BaseContext.setUserId(10001L);
        when(goalMapper.findByIdAndUserId(7L, 10001L)).thenReturn(null);

        service.deleteGoal(7L);

        verify(milestoneMapper, never()).deleteByGoalIdAndUserId(7L, 10001L);
        verify(goalMapper, never()).deleteByIdAndUserId(7L, 10001L);
    }

    @Test
    void deleteGoalUsesUserScopedMilestoneDelete() {
        BaseContext.setUserId(10001L);
        when(goalMapper.findByIdAndUserId(7L, 10001L)).thenReturn(goal());

        service.deleteGoal(7L);

        verify(milestoneMapper).deleteByGoalIdAndUserId(7L, 10001L);
        verify(goalMapper).deleteByIdAndUserId(7L, 10001L);
    }

    @Test
    void updateGoalUsesUserScopedUpdate() {
        BaseContext.setUserId(10001L);
        Goal goal = goal();
        when(goalMapper.findByIdAndUserId(7L, 10001L)).thenReturn(goal);
        GoalUpdateDTO dto = new GoalUpdateDTO();
        dto.setTitle("更新后的 AI 应用目标");

        service.updateGoal(7L, dto);

        ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
        verify(goalMapper).updateByIdAndUserId(goalCaptor.capture());
        assertEquals(10001L, goalCaptor.getValue().getUserId());
        assertEquals("更新后的 AI 应用目标", goalCaptor.getValue().getTitle());
    }

    @Test
    void updateMilestoneUsesUserScopedUpdate() {
        BaseContext.setUserId(10001L);
        GoalMilestone milestone = milestone(10001L);
        when(milestoneMapper.findByGoalIdAndIdAndUserId(7L, 11L, 10001L)).thenReturn(milestone);
        GoalMilestoneUpdateDTO dto = new GoalMilestoneUpdateDTO();
        dto.setStatus("DONE");

        service.updateMilestone(7L, 11L, dto);

        ArgumentCaptor<GoalMilestone> milestoneCaptor = ArgumentCaptor.forClass(GoalMilestone.class);
        verify(milestoneMapper).updateByIdAndUserId(milestoneCaptor.capture());
        assertEquals(10001L, milestoneCaptor.getValue().getUserId());
        assertEquals("DONE", milestoneCaptor.getValue().getStatus());
        assertEquals(100, milestoneCaptor.getValue().getProgress());
    }

    @Test
    void updateMilestoneDoesNotUpdateWhenPathGoalDoesNotOwnMilestone() {
        BaseContext.setUserId(10001L);
        GoalMilestoneUpdateDTO dto = new GoalMilestoneUpdateDTO();
        dto.setStatus("DONE");
        when(milestoneMapper.findByGoalIdAndIdAndUserId(7L, 11L, 10001L)).thenReturn(null);

        service.updateMilestone(7L, 11L, dto);

        verify(milestoneMapper, never()).updateByIdAndUserId(any());
    }

    @Test
    void generateAiAdviceFailsWhenScopedUpdateDoesNotAffectRow() {
        BaseContext.setUserId(10001L);
        when(goalMapper.findByIdAndUserId(7L, 10001L)).thenReturn(goal());
        when(milestoneMapper.findByGoalIdAndUserId(7L, 10001L)).thenReturn(List.of());
        when(pythonGoalsAdviceClient.generateGoalAdvice(any())).thenReturn(new AiAdviceVO("建议"));
        when(goalMapper.updateAiAdviceByIdAndUserId(7L, 10001L, "建议")).thenReturn(0);

        BaseException exception = assertThrows(BaseException.class, () -> service.generateAiAdvice(7L));

        assertEquals("目标不存在", exception.getMessage());
    }

    private Goal goal() {
        Goal goal = new Goal();
        goal.setId(7L);
        goal.setUserId(10001L);
        goal.setTitle("成为 AI 应用开发工程师");
        goal.setGoalDesc("补齐 Python RAG 与工程化能力");
        goal.setStatus("IN_PROGRESS");
        goal.setProgress(45);
        goal.setEta("2026年9月");
        goal.setIsPrimary(true);
        goal.setSuccessSalary("15k-25k");
        goal.setSuccessCompanies("[\"字节跳动\"]");
        goal.setSuccessCities("[\"杭州\"]");
        goal.setLongTermAspirations("[{\"title\":\"AI 应用工程师\",\"desc\":\"能独立落地 RAG 系统\"}]");
        return goal;
    }

    private GoalMilestone milestone(Long userId) {
        GoalMilestone milestone = new GoalMilestone();
        milestone.setId(userId);
        milestone.setGoalId(7L);
        milestone.setUserId(userId);
        milestone.setTitle("完成 RAG 项目");
        milestone.setMilestoneDesc("实现检索、重排和评估");
        milestone.setStatus("IN_PROGRESS");
        milestone.setProgress(40);
        milestone.setSortOrder(1);
        return milestone;
    }
}
