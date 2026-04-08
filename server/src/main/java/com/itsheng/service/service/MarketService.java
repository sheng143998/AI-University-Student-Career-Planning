package com.itsheng.service.service;

import com.itsheng.pojo.vo.MarketHotJobsVO;
import com.itsheng.pojo.vo.MarketInsightVO;
import com.itsheng.pojo.vo.MarketJobDetailVO;
import com.itsheng.pojo.vo.MarketProfileListVO;
import com.itsheng.pojo.vo.MarketTrendsVO;

public interface MarketService {
    MarketProfileListVO getProfiles(String industry, String city, String keyword, Integer page, Integer size);

    MarketTrendsVO getTrends(Long jobProfileId, String city, String timeRange);

    MarketInsightVO getInsight(Long jobProfileId, String city);

    MarketHotJobsVO getHotJobs(Integer limit, String city, String industry);

    MarketJobDetailVO getJobDetail(Long jobId);
    
    /**
     * Generate and save job profile to database
     * @param jobId job ID
     * @return generated job detail
     */
    MarketJobDetailVO generateAndSaveJobProfile(Long jobId);
    
    /**
     * Generate and save all job profiles to database
     * @return number of profiles generated
     */
    int generateAllJobProfiles();
}
