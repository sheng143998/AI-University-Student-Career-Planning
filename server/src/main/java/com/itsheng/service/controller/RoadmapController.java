package com.itsheng.service.controller;

import com.itsheng.pojo.vo.CareerPathRecommendationVO;
import com.itsheng.pojo.vo.JobDetailVO;
import com.itsheng.pojo.vo.JobSearchResultVO;
import com.itsheng.pojo.vo.JobVerticalPathDetailVO;
import com.itsheng.pojo.vo.RoadmapGraphVO;
import com.itsheng.pojo.vo.RoadmapNodeDetailVO;
import com.itsheng.pojo.vo.RoadmapSearchResultVO;
import com.itsheng.pojo.vo.UserTransitionRecommendationVO;
import com.itsheng.service.service.RoadmapService;
import com.itsheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
     * 搜索岗位（对齐前端 /api/roadmap/jobs/search）
     */
    @GetMapping("/jobs/search")
    @Operation(summary = "搜索岗位", description = "根据关键字搜索岗位（用于职业地图中心岗位定位）")
    public Result<java.util.List<JobSearchResultVO>> searchJobs(
            @Parameter(description = "搜索关键字") @RequestParam String q,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("搜索岗位: q={}, limit={}", q, limit);
        return Result.success(roadmapService.searchJobs(q, limit));
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
     * 根据岗位名称获取晋升路径（对齐前端 /api/roadmap/map/path-by-name）
     */
    @GetMapping("/map/path-by-name")
    @Operation(summary = "按岗位名称获取晋升路径", description = "根据岗位名称和级别返回垂直晋升路径")
    public Result<JobVerticalPathDetailVO> getVerticalPathByJobName(
            @Parameter(description = "岗位名称") @RequestParam String jobName,
            @Parameter(description = "岗位级别") @RequestParam(required = false) String level) {
        log.info("按岗位名称获取晋升路径: jobName={}, level={}", jobName, level);
        return Result.success(roadmapService.getVerticalPathByJobNameAndLevel(jobName, level));
    }

    /**
     * 根据岗位名称获取换岗推荐（对齐前端 /api/roadmap/recommend/transition/by-job）
     */
    @PostMapping("/recommend/transition/by-job")
    @Operation(summary = "按岗位名称获取换岗推荐", description = "根据岗位名称和级别返回换岗推荐列表")
    public Result<UserTransitionRecommendationVO> recommendTransitionByJob(
            @Parameter(description = "岗位名称") @RequestParam String jobName,
            @Parameter(description = "岗位级别") @RequestParam(required = false) String level) {
        log.info("按岗位名称获取换岗推荐: jobName={}, level={}", jobName, level);
        return Result.success(roadmapService.recommendTransitionByJobNameAndLevel(jobName, level));
    }

    /**
     * 获取岗位详情（对齐前端 /api/roadmap/map/job-detail/{id}）
     */
    @GetMapping("/map/job-detail/{id}")
    @Operation(summary = "获取岗位详情", description = "根据岗位ID返回岗位详情")
    public Result<JobDetailVO> getJobDetail(
            @Parameter(description = "岗位ID") @PathVariable Long id) {
        log.info("获取岗位详情: id={}", id);
        return Result.success(roadmapService.getJobDetail(id));
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

    /**
     * Clear personalized recommendations cache
     */
    @DeleteMapping("/recommendations/personalized/cache")
    @Operation(summary = "Clear recommendations cache", description = "Clear the cached personalized recommendations for current user")
    public Result<String> clearPersonalizedRecommendationsCache() {
        log.info("Clearing personalized recommendations cache");
        roadmapService.clearPersonalizedRecommendationsCache();
        return Result.success("Cache cleared successfully");
    }

    /**
     * 保存用户手动设置的当前岗位
     */
    @PostMapping("/user/current-job")
    @Operation(summary = "保存当前岗位", description = "保存用户手动设置的当前岗位到 Redis")
    public Result<String> saveUserCurrentJob(@RequestBody Map<String, String> request) {
        String currentJob = request.get("currentJob");
        log.info("保存用户当前岗位: {}", currentJob);
        roadmapService.saveUserCurrentJob(currentJob);
        return Result.success("当前岗位保存成功");
    }

    /**
     * 获取用户手动设置的当前岗位
     */
    @GetMapping("/user/current-job")
    @Operation(summary = "获取当前岗位", description = "获取用户手动设置的当前岗位")
    public Result<String> getUserCurrentJob() {
        String currentJob = roadmapService.getUserCurrentJob();
        log.info("获取用户当前岗位: {}", currentJob);
        return Result.success(currentJob);
    }
}
