package com.itsheng.service.controller;

import com.itsheng.common.context.BaseContext;
import com.itsheng.common.result.Result;
import com.itsheng.service.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dashboard Controller
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard 仪表盘")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取仪表盘汇总信息
     * @return 仪表盘数据
     */
    @GetMapping("/summary")
    @Operation(summary = "获取仪表盘汇总", description = "获取当前登录用户的仪表盘汇总信息，包括匹配度摘要、市场趋势、能力雷达图和行动建议")
    public Result<Map<String, Object>> getSummary() {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 获取仪表盘汇总", userId);

        Map<String, Object> result = dashboardService.getSummary(userId);
        if (result == null) {
            return Result.error("请先上传简历以生成职业分析");
        }
        return Result.success(result);
    }

    /**
     * 获取用户职业发展路径
     * @return roadmap 数据
     */
    @GetMapping("/roadmap")
    @Operation(summary = "获取用户职业发展路径", description = "获取当前登录用户的职业发展路径信息，即首页'职业进化地图'卡片数据。若不存在则自动基于目标岗位的垂直晋升路径创建")
    public Result<Map<String, Object>> getRoadmap() {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 获取职业发展路径", userId);

        Map<String, Object> result = dashboardService.getRoadmap(userId);
        if (result == null) {
            return Result.error("请先上传简历以生成职业规划");
        }
        return Result.success(result);
    }

    /**
     * 更新用户当前所在阶段
     * @param request 请求体，包含 current_step_index 字段
     * @return 更新后的 steps 列表
     */
    @PutMapping("/roadmap/current-step")
    @Operation(summary = "更新用户当前所在阶段", description = "用户编辑自己当前的职业发展阶段，选择符合自己实际情况的级别")
    public Result<List<Map<String, Object>>> updateCurrentStep(@RequestBody Map<String, Integer> request) {
        Long userId = BaseContext.getUserId();
        Integer currentStepIndex = request.get("current_step_index");

        if (currentStepIndex == null) {
            return Result.error("current_step_index 不能为空");
        }

        log.info("用户 ID:{}, 更新当前阶段，newIndex: {}", userId, currentStepIndex);

        try {
            List<Map<String, Object>> result = dashboardService.updateCurrentStep(userId, currentStepIndex);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
