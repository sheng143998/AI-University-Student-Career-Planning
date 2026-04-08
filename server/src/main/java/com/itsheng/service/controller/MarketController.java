package com.itsheng.service.controller;

import com.itsheng.common.result.Result;
import com.itsheng.pojo.vo.MarketHotJobsVO;
import com.itsheng.pojo.vo.MarketInsightVO;
import com.itsheng.pojo.vo.MarketJobDetailVO;
import com.itsheng.pojo.vo.MarketProfileListVO;
import com.itsheng.pojo.vo.MarketTrendsVO;
import com.itsheng.service.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "市场探索", description = "Market 模块接口")
public class MarketController {

    private final MarketService marketService;

    @GetMapping("/profiles")
    @Operation(summary = "获取岗位画像列表", description = "岗位画像列表（支持筛选）")
    public Result<MarketProfileListVO> getProfiles(
            @Parameter(description = "所属行业分段") @RequestParam(required = false) String industry,
            @Parameter(description = "工作城市") @RequestParam(required = false) String city,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size
    ) {
        return Result.success(marketService.getProfiles(industry, city, keyword, page, size));
    }

    @GetMapping("/trends")
    @Operation(summary = "获取市场趋势", description = "薪资/需求/更新时间")
    public Result<MarketTrendsVO> getTrends(
            @Parameter(description = "指定岗位 ID") @RequestParam(required = false, name = "job_profile_id") Long jobProfileId,
            @Parameter(description = "城市") @RequestParam(required = false) String city,
            @Parameter(description = "时间范围") @RequestParam(required = false, name = "time_range", defaultValue = "quarter") String timeRange
    ) {
        return Result.success(marketService.getTrends(jobProfileId, city, timeRange));
    }

    @GetMapping("/insight")
    @Operation(summary = "获取 AI 深度洞察", description = "AI 生成的市场洞察")
    public Result<MarketInsightVO> getInsight(
            @Parameter(description = "目标岗位") @RequestParam(required = false, name = "job_profile_id") Long jobProfileId,
            @Parameter(description = "城市") @RequestParam(required = false) String city
    ) {
        return Result.success(marketService.getInsight(jobProfileId, city));
    }

    @GetMapping("/hot-jobs")
    @Operation(summary = "获取热门岗位", description = "热门岗位画像列表")
    public Result<MarketHotJobsVO> getHotJobs(
            @Parameter(description = "返回数量") @RequestParam(required = false, defaultValue = "10") Integer limit,
            @Parameter(description = "城市") @RequestParam(required = false) String city,
            @Parameter(description = "行业") @RequestParam(required = false) String industry
    ) {
        return Result.success(marketService.getHotJobs(limit, city, industry));
    }

    @GetMapping("/jobs/{job_id}")
    @Operation(summary = "Get job detail", description = "Get job profile detail")
    public Result<MarketJobDetailVO> getJobDetail(@PathVariable("job_id") Long jobId) {
        return Result.success(marketService.getJobDetail(jobId));
    }
    
    @PostMapping("/jobs/{job_id}/generate")
    @Operation(summary = "Generate and save job profile", description = "Generate AI soft skills and save full profile to database")
    public Result<MarketJobDetailVO> generateJobProfile(@PathVariable("job_id") Long jobId) {
        return Result.success(marketService.generateAndSaveJobProfile(jobId));
    }
    
    @PostMapping("/jobs/generate-all")
    @Operation(summary = "Generate all job profiles", description = "Generate AI soft skills and save all job profiles to database")
    public Result<Integer> generateAllJobProfiles() {
        return Result.success(marketService.generateAllJobProfiles());
    }
}
