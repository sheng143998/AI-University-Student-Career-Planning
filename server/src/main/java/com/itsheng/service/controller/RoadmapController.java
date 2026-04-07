package com.itsheng.service.controller;

import com.itsheng.pojo.vo.CareerPathRecommendationVO;
import com.itsheng.pojo.vo.RoadmapGraphVO;
import com.itsheng.pojo.vo.RoadmapNodeDetailVO;
import com.itsheng.pojo.vo.RoadmapSearchResultVO;
import com.itsheng.service.service.RoadmapService;
import com.itsheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 职业地图控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
@Tag(name = "职业地图", description = "职业地图相关接口")
public class RoadmapController {

    private final RoadmapService roadmapService;

    /**
     * 搜索职业节点
     */
    @GetMapping("/search")
    @Operation(summary = "搜索职业节点", description = "根据关键字搜索职业节点")
    public Result<RoadmapSearchResultVO> searchNodes(
            @Parameter(description = "搜索关键字") @RequestParam String q,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "20") Integer limit) {
        log.info("搜索职业节点: q={}, limit={}", q, limit);
        return Result.success(roadmapService.searchNodes(q, limit));
    }

    /**
     * 获取地图图谱（垂直晋升路径）
     */
    @GetMapping("/graph")
    @Operation(summary = "获取地图图谱", description = "根据岗位类别获取垂直晋升路径图谱")
    public Result<RoadmapGraphVO> getGraph(
            @Parameter(description = "岗位类别编码") @RequestParam(required = false) String categoryCode,
            @Parameter(description = "模式：vertical/lateral") @RequestParam(defaultValue = "vertical") String mode) {
        log.info("获取地图图谱: categoryCode={}, mode={}", categoryCode, mode);
        return Result.success(roadmapService.getGraph(categoryCode, mode));
    }

    /**
     * 获取节点详情
     */
    @GetMapping("/nodes/{id}")
    @Operation(summary = "获取节点详情", description = "根据节点ID获取详细信息")
    public Result<RoadmapNodeDetailVO> getNodeDetail(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        log.info("获取节点详情: id={}", id);
        return Result.success(roadmapService.getNodeDetail(id));
    }

    /**
     * 获取个性化职业路径推荐
     */
    @GetMapping("/recommendations/personalized")
    @Operation(summary = "获取个性化职业路径推荐", description = "根据用户当前岗位和简历分析结果，推荐相似的垂直晋升路径和横向换岗路径")
    public Result<CareerPathRecommendationVO> getPersonalizedRecommendations() {
        log.info("获取个性化职业路径推荐");
        return Result.success(roadmapService.getPersonalizedRecommendations());
    }
}
