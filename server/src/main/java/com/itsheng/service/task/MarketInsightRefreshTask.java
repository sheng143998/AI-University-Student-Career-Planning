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
 * AIå¸åºæ´å¯å®æ¶å·æ°ä»»å¡¡
 * æ¯å¤©åæ¨¨2ç¹æ§è¡ï¼æ´æ°ææå²ä½çAIæ´å¯ç¼å­
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketInsightRefreshTask {

    private final MarketServiceImpl marketService;
    private final JobCategoryMapper jobCategoryMapper;

    /**
     * æ¯å¤©åæ¨¨2ç¹æ§è¡ï¼å·æ°ææå²ä½çAIæ´å¯ç¼å­
     * cron: ç§ åé æ¶ æ¥ æ æ å¹
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshAllInsights() {
        log.info("===== å¼å§æ§è¡AIå¸åºæ´å¯å®æ¶å·æ°ä»»å¡ =====");
        
        try {
            // è·åææå²ä½
            List<JobCategory> allJobs = jobCategoryMapper.selectAll();
            int successCount = 0;
            int failCount = 0;
            
            for (JobCategory job : allJobs) {
                try {
                    marketService.refreshInsightCache(job.getId());
                    successCount++;
                } catch (Exception e) {
                    log.warn("å·æ°å²ä½æ´å¯å¤±è´¥: jobId={}, error={}", job.getId(), e.getMessage());
                    failCount++;
                }
            }
            
            log.info("===== AIå¸åºæ´å¯å®æ¶å·æ°å®æ: æå={}, å¤±è´¥={}, æ»æ°={} =====", 
                    successCount, failCount, allJobs.size());
                    
        } catch (Exception e) {
            log.error("AIå¸åºæ´å¯å®æ¶å·æ°ä»»å¡æ§è¡å¤±è´¥", e);
        }
    }
}
