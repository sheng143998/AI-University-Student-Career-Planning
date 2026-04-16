package com.itsheng.service.task;

import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.service.Impl.MarketServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI市场洞察定时刷新任务
 * 每天凌晨2点执行，更新所有岗位的AI洞察缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketInsightRefreshTask {

    private final MarketServiceImpl marketService;
    private final JobCategoryMapper jobCategoryMapper;

    /**
     * 每天凌晨2点执行，更新所有岗位的AI洞察缓存
     * cron: 秒 分 时 日 月 星期
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshAllInsights() {
        log.info("===== 开始执行AI市场洞察定时刷新任务 =====");

        try {
            // 获取所有岗位
            List<JobCategory> allJobs = jobCategoryMapper.selectAll();
            int successCount = 0;
            int failCount = 0;

            for (JobCategory job : allJobs) {
                try {
                    marketService.refreshInsightCache(job.getId());
                    successCount++;
                } catch (Exception e) {
                    log.warn("更新岗位洞察失败: jobId={}, error={}", job.getId(), e.getMessage());
                    failCount++;
                }
            }

            log.info("===== AI市场洞察定时刷新完成: 成功={}, 失败={}, 总数={} =====",
                    successCount, failCount, allJobs.size());

        } catch (Exception e) {
            log.error("AI市场洞察定时刷新任务执行失败", e);
        }
    }
}
