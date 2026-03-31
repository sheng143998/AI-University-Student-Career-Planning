package com.itsheng.service.controller;

import com.itsheng.common.result.Result;
import com.itsheng.pojo.dto.GoalCreateDTO;
import com.itsheng.pojo.dto.GoalMilestoneCreateDTO;
import com.itsheng.pojo.dto.GoalMilestoneUpdateDTO;
import com.itsheng.pojo.dto.GoalUpdateDTO;
import com.itsheng.pojo.vo.GoalDetailVO;
import com.itsheng.pojo.vo.GoalsOverviewVO;
import com.itsheng.pojo.vo.IdVO;
import com.itsheng.service.service.GoalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "目标管理接口")
public class GoalsController {

    private final GoalsService goalsService;

    @GetMapping("/overview")
    @Operation(summary = "获取目标总览")
    public Result<GoalsOverviewVO> overview() {
        log.info("获取目标总览");
        GoalsOverviewVO overview = goalsService.overview();
        return Result.success(overview);
    }

    @PostMapping
    @Operation(summary = "创建目标")
    public Result<IdVO> create(@RequestBody GoalCreateDTO dto) {
        log.info("创建目标: {}", dto);
        IdVO idVO = goalsService.createGoal(dto);
        return Result.success(idVO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取目标详情")
    public Result<GoalDetailVO> detail(@PathVariable Long id) {
        log.info("获取目标详情, id: {}", id);
        GoalDetailVO detail = goalsService.getGoalDetail(id);
        return Result.success(detail);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新目标")
    public Result<Void> update(@PathVariable Long id, @RequestBody GoalUpdateDTO dto) {
        log.info("更新目标, id: {}, dto: {}", id, dto);
        goalsService.updateGoal(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除目标")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除目标, id: {}", id);
        goalsService.deleteGoal(id);
        return Result.success();
    }

    @PostMapping("/{id}/milestones")
    @Operation(summary = "创建里程碑")
    public Result<IdVO> createMilestone(@PathVariable Long id, @RequestBody GoalMilestoneCreateDTO dto) {
        log.info("创建里程碑, goalId: {}, dto: {}", id, dto);
        IdVO idVO = goalsService.createMilestone(id, dto);
        return Result.success(idVO);
    }

    @PatchMapping("/{id}/milestones/{msId}")
    @Operation(summary = "更新里程碑")
    public Result<Void> updateMilestone(@PathVariable Long id, @PathVariable Long msId, @RequestBody GoalMilestoneUpdateDTO dto) {
        log.info("更新里程碑, goalId: {}, milestoneId: {}, dto: {}", id, msId, dto);
        goalsService.updateMilestone(msId, dto);
        return Result.success();
    }
}
