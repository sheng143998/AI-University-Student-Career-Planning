package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.vo.*;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.service.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketServiceImpl implements MarketService {

    private final JobCategoryMapper jobCategoryMapper;
    private final ObjectMapper objectMapper;
    private final ChatClient jobClassificationChatClient;
    private final StringRedisTemplate redisTemplate;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final String REDIS_KEY_PREFIX = "market:insight:";
    private static final Duration REDIS_TTL = Duration.ofHours(24); // 24小时过期

    @Override
    public MarketProfileListVO getProfiles(String industry, String city, String keyword, Integer page, Integer size) {
        // job table does not have city/industry, so we ignore those filters
        List<JobCategory> allJobs = jobCategoryMapper.selectAll();
        
        // Filter by keyword if provided
        List<JobCategory> filteredJobs = allJobs;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            filteredJobs = allJobs.stream()
                    .filter(job -> job.getJobCategoryName() != null && 
                            job.getJobCategoryName().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }
        
        // Pagination
        int total = filteredJobs.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        
        List<MarketProfileItemVO> items = new ArrayList<>();
        if (start < total) {
            List<JobCategory> pagedJobs = filteredJobs.subList(start, end);
            items = pagedJobs.stream()
                    .map(this::convertToProfileItem)
                    .collect(Collectors.toList());
        }
        
        MarketProfileListVO result = new MarketProfileListVO();
        result.setItems(items);
        result.setTotal((long) total);
        result.setPage(page);
        result.setSize(size);
        result.setUpdatedAt(LocalDateTime.now().format(ISO_FORMATTER));
        return result;
    }

    @Override
    public MarketTrendsVO getTrends(Long jobProfileId, String city, String timeRange) {
        // If no jobProfileId, return aggregated trends from all jobs
        if (jobProfileId == null) {
            return buildAggregatedTrends();
        }
        
        JobCategory job = jobCategoryMapper.selectById(jobProfileId);
        if (job == null) {
            return buildAggregatedTrends();
        }
        
        return buildTrendsForJob(job);
    }

    @Override
    public MarketInsightVO getInsight(Long jobProfileId, String city) {
        MarketInsightVO insight = new MarketInsightVO();
        
        if (jobProfileId != null) {
            JobCategory job = jobCategoryMapper.selectById(jobProfileId);
            if (job != null) {
                insight.setJobProfileId(jobProfileId);
                insight.setJobName(job.getJobCategoryName());
            }
        } else {
            insight.setJobProfileId(1L);
            insight.setJobName("Java Development Engineer");
        }
        
        insight.setCity(city != null ? city : "Beijing");
        
        // Try to get from Redis cache first
        String redisKey = REDIS_KEY_PREFIX + (jobProfileId != null ? jobProfileId : "default");
        try {
            String cachedInsight = redisTemplate.opsForValue().get(redisKey);
            if (cachedInsight != null && !cachedInsight.isEmpty()) {
                MarketInsightContentVO content = objectMapper.readValue(cachedInsight, MarketInsightContentVO.class);
                insight.setInsight(content);
                insight.setUpdatedAt(LocalDateTime.now().format(ISO_FORMATTER));
                log.debug("Retrieved insight from Redis cache for jobProfileId: {}", jobProfileId);
                return insight;
            }
        } catch (Exception e) {
            log.warn("Failed to read insight from Redis: {}", e.getMessage());
        }
        
        // Generate new insight via AI
        MarketInsightContentVO content = buildInsightContent(jobProfileId);
        insight.setInsight(content);
        insight.setUpdatedAt(LocalDateTime.now().format(ISO_FORMATTER));
        
        // Cache to Redis
        try {
            String contentJson = objectMapper.writeValueAsString(content);
            redisTemplate.opsForValue().set(redisKey, contentJson, REDIS_TTL);
            log.info("Cached insight to Redis for jobProfileId: {}", jobProfileId);
        } catch (Exception e) {
            log.warn("Failed to cache insight to Redis: {}", e.getMessage());
        }
        
        return insight;
    }
    
    /**
     * Refresh insight cache for a specific job (called by scheduled task)
     */
    public void refreshInsightCache(Long jobProfileId) {
        String redisKey = REDIS_KEY_PREFIX + jobProfileId;
        try {
            MarketInsightContentVO content = buildInsightContent(jobProfileId);
            String contentJson = objectMapper.writeValueAsString(content);
            redisTemplate.opsForValue().set(redisKey, contentJson, REDIS_TTL);
            log.info("Refreshed insight cache for jobProfileId: {}", jobProfileId);
        } catch (Exception e) {
            log.error("Failed to refresh insight cache for jobProfileId {}: {}", jobProfileId, e.getMessage());
        }
    }

    @Override
    public MarketHotJobsVO getHotJobs(Integer limit, String city, String industry) {
        // job table does not have city/industry, ignore filters
        List<JobCategory> allJobs = jobCategoryMapper.selectAll();
        
        // Sort by sourceJobCount as a proxy for "hotness"
        List<JobCategory> hotJobs = allJobs.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getSourceJobCount() != null ? b.getSourceJobCount() : 0,
                        a.getSourceJobCount() != null ? a.getSourceJobCount() : 0))
                .limit(limit != null ? limit : 10)
                .collect(Collectors.toList());
        
        List<MarketHotJobItemVO> items = hotJobs.stream()
                .map(this::convertToHotJobItem)
                .collect(Collectors.toList());
        
        MarketHotJobsVO result = new MarketHotJobsVO();
        result.setItems(items);
        result.setUpdatedAt(LocalDateTime.now().format(ISO_FORMATTER));
        return result;
    }

    @Override
    public MarketJobDetailVO getJobDetail(Long jobId) {
        if (jobId == null) {
            return null;
        }
        
        JobCategory job = jobCategoryMapper.selectById(jobId);
        if (job == null) {
            return null;
        }
        
        return convertToJobDetail(job);
    }
    
    @Override
    public MarketJobDetailVO generateAndSaveJobProfile(Long jobId) {
        if (jobId == null) {
            return null;
        }
        
        JobCategory job = jobCategoryMapper.selectById(jobId);
        if (job == null) {
            return null;
        }
        
        // Generate full job detail with AI soft skills
        MarketJobDetailVO detail = convertToJobDetail(job);
        
        // Build job profile JSON for storage
        try {
            Map<String, Object> profileMap = new LinkedHashMap<>();
            profileMap.put("industrySegment", detail.getIndustrySegment());
            profileMap.put("city", detail.getCities() != null && !detail.getCities().isEmpty() ? detail.getCities().get(0) : "Beijing");
            profileMap.put("coreSkills", detail.getRequiredSkills());
            profileMap.put("certificateRequirements", detail.getCertificateRequirements());
            profileMap.put("companyBenefits", detail.getCompanyBenefits());
            profileMap.put("demandLevel", detail.getDemandAnalysis() != null ? detail.getDemandAnalysis().getLevel() : "Medium");
            
            // Capability requirements
            if (detail.getCapabilityRequirements() != null) {
                Map<String, Integer> capsMap = new LinkedHashMap<>();
                MarketCapabilityRequirementsVO caps = detail.getCapabilityRequirements();
                if (caps.getInnovation() != null) capsMap.put("innovation", caps.getInnovation());
                if (caps.getLearning() != null) capsMap.put("learning", caps.getLearning());
                if (caps.getResilience() != null) capsMap.put("resilience", caps.getResilience());
                if (caps.getCommunication() != null) capsMap.put("communication", caps.getCommunication());
                if (caps.getInternship() != null) capsMap.put("internship", caps.getInternship());
                profileMap.put("capabilityRequirements", capsMap);
            }
            
            // Soft skills (AI generated)
            if (detail.getSoftSkills() != null && !detail.getSoftSkills().isEmpty()) {
                profileMap.put("softSkills", detail.getSoftSkills());
            }
            
            // Career path
            if (detail.getCareerPath() != null) {
                profileMap.put("careerPath", detail.getCareerPath());
            }
            
            // Salary range
            if (detail.getSalaryRange() != null) {
                Map<String, Object> salaryMap = new LinkedHashMap<>();
                salaryMap.put("min", detail.getSalaryRange().getMin());
                salaryMap.put("max", detail.getSalaryRange().getMax());
                salaryMap.put("currency", detail.getSalaryRange().getCurrency());
                salaryMap.put("unit", detail.getSalaryRange().getUnit());
                profileMap.put("salaryRange", salaryMap);
            }
            
            // Education requirement
            profileMap.put("educationRequirement", detail.getEducationRequirement());
            
            // Experience range
            if (detail.getExperienceRange() != null) {
                Map<String, Object> expMap = new LinkedHashMap<>();
                expMap.put("min", detail.getExperienceRange().getMin());
                expMap.put("max", detail.getExperienceRange().getMax());
                expMap.put("unit", detail.getExperienceRange().getUnit());
                profileMap.put("experienceRange", expMap);
            }
            
            // Convert to JSON and save
            String profileJson = objectMapper.writeValueAsString(profileMap);
            job.setJobProfile(profileJson);
            jobCategoryMapper.update(job);
            
            log.info("Generated and saved job profile for job {}: {} chars", jobId, profileJson.length());
            
        } catch (Exception e) {
            log.error("Failed to save job profile for job {}: {}", jobId, e.getMessage());
        }
        
        return detail;
    }
    
    @Override
    public int generateAllJobProfiles() {
        List<JobCategory> allJobs = jobCategoryMapper.selectAll();
        int count = 0;
        
        for (JobCategory job : allJobs) {
            try {
                // Generate full job detail with AI soft skills
                MarketJobDetailVO detail = convertToJobDetail(job);
                
                // Build job profile JSON for storage
                Map<String, Object> profileMap = new LinkedHashMap<>();
                profileMap.put("industrySegment", detail.getIndustrySegment());
                profileMap.put("city", detail.getCities() != null && !detail.getCities().isEmpty() ? detail.getCities().get(0) : "Beijing");
                profileMap.put("coreSkills", detail.getRequiredSkills());
                profileMap.put("certificateRequirements", detail.getCertificateRequirements());
                profileMap.put("companyBenefits", detail.getCompanyBenefits());
                profileMap.put("demandLevel", detail.getDemandAnalysis() != null ? detail.getDemandAnalysis().getLevel() : "Medium");
                
                // Capability requirements
                if (detail.getCapabilityRequirements() != null) {
                    Map<String, Integer> capsMap = new LinkedHashMap<>();
                    MarketCapabilityRequirementsVO caps = detail.getCapabilityRequirements();
                    if (caps.getInnovation() != null) capsMap.put("innovation", caps.getInnovation());
                    if (caps.getLearning() != null) capsMap.put("learning", caps.getLearning());
                    if (caps.getResilience() != null) capsMap.put("resilience", caps.getResilience());
                    if (caps.getCommunication() != null) capsMap.put("communication", caps.getCommunication());
                    if (caps.getInternship() != null) capsMap.put("internship", caps.getInternship());
                    profileMap.put("capabilityRequirements", capsMap);
                }
                
                // Soft skills (AI generated)
                if (detail.getSoftSkills() != null && !detail.getSoftSkills().isEmpty()) {
                    profileMap.put("softSkills", detail.getSoftSkills());
                }
                
                // Career path
                if (detail.getCareerPath() != null) {
                    profileMap.put("careerPath", detail.getCareerPath());
                }
                
                // Salary range
                if (detail.getSalaryRange() != null) {
                    Map<String, Object> salaryMap = new LinkedHashMap<>();
                    salaryMap.put("min", detail.getSalaryRange().getMin());
                    salaryMap.put("max", detail.getSalaryRange().getMax());
                    salaryMap.put("currency", detail.getSalaryRange().getCurrency());
                    salaryMap.put("unit", detail.getSalaryRange().getUnit());
                    profileMap.put("salaryRange", salaryMap);
                }
                
                // Education requirement
                profileMap.put("educationRequirement", detail.getEducationRequirement());
                
                // Experience range
                if (detail.getExperienceRange() != null) {
                    Map<String, Object> expMap = new LinkedHashMap<>();
                    expMap.put("min", detail.getExperienceRange().getMin());
                    expMap.put("max", detail.getExperienceRange().getMax());
                    expMap.put("unit", detail.getExperienceRange().getUnit());
                    profileMap.put("experienceRange", expMap);
                }
                
                // Convert to JSON and save
                String profileJson = objectMapper.writeValueAsString(profileMap);
                job.setJobProfile(profileJson);
                jobCategoryMapper.update(job);
                
                count++;
                log.info("Generated job profile {}/{} for job: {}", count, allJobs.size(), job.getJobCategoryName());
                
            } catch (Exception e) {
                log.error("Failed to generate job profile for job {}: {}", job.getId(), e.getMessage());
            }
        }
        
        log.info("Completed generating {} job profiles out of {}", count, allJobs.size());
        return count;
    }

    // ==================== Private Helper Methods ====================

    private MarketProfileItemVO convertToProfileItem(JobCategory job) {
        MarketProfileItemVO item = new MarketProfileItemVO();
        item.setId(job.getId());
        item.setJobName(job.getJobCategoryName());
        
        // Try to use jobProfile JSON if available
        if (job.getJobProfile() != null && !job.getJobProfile().isEmpty()) {
            try {
                Map<String, Object> profileMap = objectMapper.readValue(job.getJobProfile(), 
                        new TypeReference<Map<String, Object>>() {});
                
                String industrySegment = (String) profileMap.get("industrySegment");
                item.setIndustrySegment(industrySegment != null ? industrySegment : deriveIndustrySegment(job.getJobCategoryCode()));
                String city = (String) profileMap.get("city");
                item.setCity(city != null ? city : "Beijing");
                
                // Core skills from profile
                Object skillsObj = profileMap.get("coreSkills");
                if (skillsObj instanceof List) {
                    item.setCoreSkills(((List<?>) skillsObj).stream()
                            .map(Object::toString).collect(Collectors.toList()));
                } else {
                    item.setCoreSkills(parseJsonToList(job.getRequiredSkills()));
                }
                
                // Certificate requirements from profile
                Object certsObj = profileMap.get("certificateRequirements");
                if (certsObj instanceof List) {
                    item.setCertificateRequirements(((List<?>) certsObj).stream()
                            .map(Object::toString).collect(Collectors.toList()));
                } else {
                    item.setCertificateRequirements(generateCertificates(job.getJobCategoryCode()));
                }
                
                // Capability requirements from profile
                Object capsObj = profileMap.get("capabilityRequirements");
                if (capsObj instanceof Map) {
                    MarketCapabilityRequirementsVO caps = new MarketCapabilityRequirementsVO();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> capsMap = (Map<String, Object>) capsObj;
                    caps.setInnovation(getIntValue(capsMap, "innovation"));
                    caps.setLearning(getIntValue(capsMap, "learning"));
                    caps.setResilience(getIntValue(capsMap, "resilience"));
                    caps.setCommunication(getIntValue(capsMap, "communication"));
                    caps.setInternship(getIntValue(capsMap, "internship"));
                    item.setCapabilityRequirements(caps);
                } else {
                    item.setCapabilityRequirements(generateCapabilities(job.getJobLevel()));
                }
                
                String demandLevel = (String) profileMap.get("demandLevel");
                item.setDemandLevel(demandLevel != null ? demandLevel : calculateDemandLevel(job.getSourceJobCount()));
            } catch (Exception e) {
                log.warn("Failed to parse jobProfile JSON for job {}: {}", job.getId(), e.getMessage());
                // Fall back to default derivation
                item.setIndustrySegment(deriveIndustrySegment(job.getJobCategoryCode()));
                item.setCity("Beijing");
                item.setCoreSkills(parseJsonToList(job.getRequiredSkills()));
                item.setCertificateRequirements(generateCertificates(job.getJobCategoryCode()));
                item.setCapabilityRequirements(generateCapabilities(job.getJobLevel()));
                item.setDemandLevel(calculateDemandLevel(job.getSourceJobCount()));
            }
        } else {
            // No jobProfile, use derived values
            item.setIndustrySegment(deriveIndustrySegment(job.getJobCategoryCode()));
            item.setCity("Beijing");
            item.setCoreSkills(parseJsonToList(job.getRequiredSkills()));
            item.setCertificateRequirements(generateCertificates(job.getJobCategoryCode()));
            item.setCapabilityRequirements(generateCapabilities(job.getJobLevel()));
            item.setDemandLevel(calculateDemandLevel(job.getSourceJobCount()));
        }
        
        // Salary range
        MarketSalaryRangeVO salaryRange = new MarketSalaryRangeVO();
        salaryRange.setMin(job.getMinSalary());
        salaryRange.setMax(job.getMaxSalary());
        salaryRange.setCurrency("CNY");
        salaryRange.setUnit(job.getSalaryUnit() != null ? job.getSalaryUnit().toLowerCase() : "month");
        item.setSalaryRange(salaryRange);
        
        // Experience range
        MarketExperienceRangeVO expRange = new MarketExperienceRangeVO();
        expRange.setMin(job.getRequiredExperienceYears() != null ? job.getRequiredExperienceYears() : 0);
        expRange.setMax(job.getRequiredExperienceYears() != null ? job.getRequiredExperienceYears() + 2 : 2);
        expRange.setUnit("years");
        item.setExperienceRange(expRange);
        
        item.setUpdatedAt(job.getUpdatedAt() != null ? 
                job.getUpdatedAt().format(ISO_FORMATTER) : 
                LocalDateTime.now().format(ISO_FORMATTER));
        
        return item;
    }
    
    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 50;
    }

    private MarketHotJobItemVO convertToHotJobItem(JobCategory job) {
        MarketHotJobItemVO item = new MarketHotJobItemVO();
        item.setId(job.getId());
        item.setJobName(job.getJobCategoryName());
        item.setIndustrySegment(deriveIndustrySegment(job.getJobCategoryCode()));
        item.setCity("Beijing");
        item.setTag(determineTag(job));
        
        MarketSalaryRangeVO salaryRange = new MarketSalaryRangeVO();
        salaryRange.setMin(job.getMinSalary());
        salaryRange.setMax(job.getMaxSalary());
        salaryRange.setCurrency("CNY");
        salaryRange.setUnit(job.getSalaryUnit() != null ? job.getSalaryUnit().toLowerCase() : "month");
        item.setSalaryRange(salaryRange);
        
        item.setDemandLevel(calculateDemandLevel(job.getSourceJobCount()));
        item.setHighlights(generateHighlights(job));
        item.setCoreSkills(parseJsonToList(job.getRequiredSkills()).stream().limit(3).collect(Collectors.toList()));
        item.setGrowthRate(calculateGrowthRate(job));
        item.setIcon(determineIcon(job.getJobCategoryCode()));
        
        return item;
    }

    private MarketJobDetailVO convertToJobDetail(JobCategory job) {
        MarketJobDetailVO detail = new MarketJobDetailVO();
        detail.setId(job.getId());
        detail.setJobName(job.getJobCategoryName());
        detail.setIndustrySegment(deriveIndustrySegment(job.getJobCategoryCode()));
        detail.setDescription(job.getJobDescription());
        detail.setCities(Arrays.asList("Beijing", "Shanghai", "Shenzhen", "Hangzhou"));
        
        // Try to load from job_profile field first (no AI call needed)
        if (job.getJobProfile() != null && !job.getJobProfile().isEmpty()) {
            try {
                Map<String, Object> profileMap = objectMapper.readValue(job.getJobProfile(), 
                        new TypeReference<Map<String, Object>>() {});
                
                // Industry segment
                String industrySegment = (String) profileMap.get("industrySegment");
                detail.setIndustrySegment(industrySegment != null ? industrySegment : deriveIndustrySegment(job.getJobCategoryCode()));
                
                // Salary range
                MarketSalarySnapshotVO salaryRange = new MarketSalarySnapshotVO();
                Object salaryObj = profileMap.get("salaryRange");
                if (salaryObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> salaryMap = (Map<String, Object>) salaryObj;
                    salaryRange.setMin(getBigDecimalValue(salaryMap, "min"));
                    salaryRange.setMax(getBigDecimalValue(salaryMap, "max"));
                    String currency = (String) salaryMap.get("currency");
                    salaryRange.setCurrency(currency != null ? currency : "CNY");
                    String unit = (String) salaryMap.get("unit");
                    salaryRange.setUnit(unit != null ? unit : "month");
                } else {
                    salaryRange.setMin(job.getMinSalary());
                    salaryRange.setMax(job.getMaxSalary());
                    if (job.getMinSalary() != null && job.getMaxSalary() != null) {
                        salaryRange.setAvg(job.getMinSalary().add(job.getMaxSalary()).divide(new BigDecimal("2"), RoundingMode.HALF_UP));
                    }
                    salaryRange.setCurrency("CNY");
                    salaryRange.setUnit(job.getSalaryUnit() != null ? job.getSalaryUnit().toLowerCase() : "month");
                }
                detail.setSalaryRange(salaryRange);
                
                // Experience range
                MarketExperienceRangeVO expRange = new MarketExperienceRangeVO();
                Object expObj = profileMap.get("experienceRange");
                if (expObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> expMap = (Map<String, Object>) expObj;
                    expRange.setMin(getIntValue(expMap, "min"));
                    expRange.setMax(getIntValue(expMap, "max"));
                    String unit = (String) expMap.get("unit");
                    expRange.setUnit(unit != null ? unit : "years");
                } else {
                    expRange.setMin(job.getRequiredExperienceYears() != null ? job.getRequiredExperienceYears() : 0);
                    expRange.setMax(job.getRequiredExperienceYears() != null ? job.getRequiredExperienceYears() + 2 : 2);
                    expRange.setUnit("years");
                }
                detail.setExperienceRange(expRange);
                
                // Education requirement
                String eduReq = (String) profileMap.get("educationRequirement");
                detail.setEducationRequirement(eduReq != null ? eduReq : determineEducationRequirement(job.getJobLevel()));
                
                // Core skills / required skills
                Object skillsObj = profileMap.get("coreSkills");
                if (skillsObj instanceof List) {
                    List<String> requiredSkills = ((List<?>) skillsObj).stream()
                            .map(Object::toString).collect(Collectors.toList());
                    detail.setRequiredSkills(requiredSkills);
                    
                    // coreSkills - with proficiency
                    List<MarketSkillRequirementVO> skillReqs = requiredSkills.stream()
                            .map(skill -> {
                                MarketSkillRequirementVO req = new MarketSkillRequirementVO();
                                req.setName(skill);
                                req.setProficiencyRequired(70 + new Random().nextInt(30));
                                return req;
                            })
                            .collect(Collectors.toList());
                    detail.setCoreSkills(skillReqs);
                } else {
                    List<String> requiredSkills = parseJsonToList(job.getRequiredSkills());
                    detail.setRequiredSkills(requiredSkills);
                    detail.setCoreSkills(requiredSkills.stream()
                            .map(skill -> {
                                MarketSkillRequirementVO req = new MarketSkillRequirementVO();
                                req.setName(skill);
                                req.setProficiencyRequired(70 + new Random().nextInt(30));
                                return req;
                            })
                            .collect(Collectors.toList()));
                }
                
                // Capability requirements
                Object capsObj = profileMap.get("capabilityRequirements");
                MarketCapabilityRequirementsVO caps = new MarketCapabilityRequirementsVO();
                if (capsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> capsMap = (Map<String, Object>) capsObj;
                    caps.setInnovation(getIntValue(capsMap, "innovation"));
                    caps.setLearning(getIntValue(capsMap, "learning"));
                    caps.setResilience(getIntValue(capsMap, "resilience"));
                    caps.setCommunication(getIntValue(capsMap, "communication"));
                    caps.setInternship(getIntValue(capsMap, "internship"));
                } else {
                    caps = generateCapabilities(job.getJobLevel());
                }
                detail.setCapabilityRequirements(caps);
                
                // Soft skills - from profile (no AI call)
                Object softSkillsObj = profileMap.get("softSkills");
                if (softSkillsObj instanceof List) {
                    List<SoftSkillItemVO> softSkills = new ArrayList<>();
                    for (Object item : (List<?>) softSkillsObj) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> skillMap = (Map<String, Object>) item;
                            SoftSkillItemVO skill = new SoftSkillItemVO();
                            skill.setName((String) skillMap.get("name"));
                            skill.setScore(getIntValue(skillMap, "score"));
                            skill.setDescription((String) skillMap.get("description"));
                            Object evidenceObj = skillMap.get("evidence");
                            if (evidenceObj instanceof List) {
                                skill.setEvidence(((List<?>) evidenceObj).stream()
                                        .map(Object::toString).collect(Collectors.toList()));
                            }
                            softSkills.add(skill);
                        }
                    }
                    detail.setSoftSkills(softSkills);
                } else {
                    // Fallback to basic soft skills (no AI call)
                    detail.setSoftSkills(generateBasicSoftSkills(caps));
                }
                
                // Certificate requirements
                Object certsObj = profileMap.get("certificateRequirements");
                if (certsObj instanceof List) {
                    detail.setCertificateRequirements(((List<?>) certsObj).stream()
                            .map(Object::toString).collect(Collectors.toList()));
                } else {
                    detail.setCertificateRequirements(generateCertificates(job.getJobCategoryCode()));
                }
                
                // Company benefits
                Object benefitsObj = profileMap.get("companyBenefits");
                if (benefitsObj instanceof List) {
                    detail.setCompanyBenefits(((List<?>) benefitsObj).stream()
                            .map(Object::toString).collect(Collectors.toList()));
                } else {
                    detail.setCompanyBenefits(Arrays.asList("Five social insurance and one housing fund", "Year-end bonus", "Paid annual leave", "Training opportunities"));
                }
                
                // Career path
                Object careerPathObj = profileMap.get("careerPath");
                if (careerPathObj instanceof Map) {
                    MarketCareerPathVO careerPath = new MarketCareerPathVO();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cpMap = (Map<String, Object>) careerPathObj;
                    Object verticalObj = cpMap.get("vertical");
                    if (verticalObj instanceof List) {
                        careerPath.setVertical(((List<?>) verticalObj).stream()
                                .map(Object::toString).collect(Collectors.toList()));
                    }
                    Object lateralObj = cpMap.get("lateral");
                    if (lateralObj instanceof List) {
                        careerPath.setLateral(((List<?>) lateralObj).stream()
                                .map(Object::toString).collect(Collectors.toList()));
                    }
                    detail.setCareerPath(careerPath);
                } else {
                    MarketCareerPathVO careerPath = new MarketCareerPathVO();
                    careerPath.setVertical(buildVerticalPath(job));
                    careerPath.setLateral(buildLateralPath(job));
                    detail.setCareerPath(careerPath);
                }
                
                // Demand analysis
                detail.setDemandAnalysis(new MarketDemandAnalysisVO());
                String demandLevel = (String) profileMap.get("demandLevel");
                detail.getDemandAnalysis().setLevel(demandLevel != null ? demandLevel : calculateDemandLevel(job.getSourceJobCount()));
                detail.getDemandAnalysis().setGrowthRate(calculateGrowthRate(job));
                detail.getDemandAnalysis().setTrend("UP");
                
                detail.setUpdatedAt(job.getUpdatedAt() != null ? 
                        job.getUpdatedAt().format(ISO_FORMATTER) : 
                        LocalDateTime.now().format(ISO_FORMATTER));
                
                return detail;
                
            } catch (Exception e) {
                log.warn("Failed to parse jobProfile JSON for job {}: {}", job.getId(), e.getMessage());
                // Fall through to default generation
            }
        }
        
        // Default generation (no AI call, use fallback soft skills)
        MarketSalarySnapshotVO salaryRange = new MarketSalarySnapshotVO();
        salaryRange.setMin(job.getMinSalary());
        salaryRange.setMax(job.getMaxSalary());
        if (job.getMinSalary() != null && job.getMaxSalary() != null) {
            salaryRange.setAvg(job.getMinSalary().add(job.getMaxSalary()).divide(new BigDecimal("2"), RoundingMode.HALF_UP));
        }
        salaryRange.setCurrency("CNY");
        salaryRange.setUnit(job.getSalaryUnit() != null ? job.getSalaryUnit().toLowerCase() : "month");
        detail.setSalaryRange(salaryRange);
        
        MarketExperienceRangeVO expRange = new MarketExperienceRangeVO();
        expRange.setMin(job.getRequiredExperienceYears() != null ? job.getRequiredExperienceYears() : 0);
        expRange.setMax(job.getRequiredExperienceYears() != null ? job.getRequiredExperienceYears() + 2 : 2);
        expRange.setUnit("years");
        detail.setExperienceRange(expRange);
        
        detail.setEducationRequirement(determineEducationRequirement(job.getJobLevel()));
        
        // requiredSkills - from job table
        List<String> requiredSkills = parseJsonToList(job.getRequiredSkills());
        detail.setRequiredSkills(requiredSkills);
        
        // coreSkills - with proficiency
        List<MarketSkillRequirementVO> skillReqs = requiredSkills.stream()
                .map(skill -> {
                    MarketSkillRequirementVO req = new MarketSkillRequirementVO();
                    req.setName(skill);
                    req.setProficiencyRequired(70 + new Random().nextInt(30));
                    return req;
                })
                .collect(Collectors.toList());
        detail.setCoreSkills(skillReqs);
        
        // capabilityRequirements
        MarketCapabilityRequirementsVO caps = generateCapabilities(job.getJobLevel());
        detail.setCapabilityRequirements(caps);
        
        // softSkills - use fallback (no AI call)
        detail.setSoftSkills(generateBasicSoftSkills(caps));
        
        detail.setCertificateRequirements(generateCertificates(job.getJobCategoryCode()));
        detail.setCompanyBenefits(Arrays.asList("Five social insurance and one housing fund", "Year-end bonus", "Paid annual leave", "Training opportunities"));
        
        MarketCareerPathVO careerPath = new MarketCareerPathVO();
        careerPath.setVertical(buildVerticalPath(job));
        careerPath.setLateral(buildLateralPath(job));
        detail.setCareerPath(careerPath);
        
        MarketDemandAnalysisVO demandAnalysis = new MarketDemandAnalysisVO();
        demandAnalysis.setLevel(calculateDemandLevel(job.getSourceJobCount()));
        demandAnalysis.setGrowthRate(calculateGrowthRate(job));
        demandAnalysis.setTrend("UP");
        detail.setDemandAnalysis(demandAnalysis);
        
        detail.setUpdatedAt(job.getUpdatedAt() != null ? 
                job.getUpdatedAt().format(ISO_FORMATTER) : 
                LocalDateTime.now().format(ISO_FORMATTER));
        
        return detail;
    }
    
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return null;
    }

    /**
     * Generate soft skills via AI based on job data
     */
    private List<SoftSkillItemVO> generateSoftSkills(JobCategory job, MarketCapabilityRequirementsVO caps) {
        try {
            // Build prompt with job context
            String jobContext = String.format("""
                {
                  "jobName": "%s",
                  "jobLevel": "%s",
                  "requiredSkills": %s,
                  "description": "%s",
                  "capabilityScores": {
                    "innovation": %d,
                    "learning": %d,
                    "resilience": %d,
                    "communication": %d,
                    "internship": %d
                  }
                }
                """,
                job.getJobCategoryName(),
                job.getJobLevel(),
                job.getRequiredSkills() != null ? job.getRequiredSkills() : "[]",
                job.getJobDescription() != null ? job.getJobDescription().replace("\"", "\\\"").replace("\n", " ") : "",
                caps.getInnovation() != null ? caps.getInnovation() : 50,
                caps.getLearning() != null ? caps.getLearning() : 50,
                caps.getResilience() != null ? caps.getResilience() : 50,
                caps.getCommunication() != null ? caps.getCommunication() : 50,
                caps.getInternship() != null ? caps.getInternship() : 50
            );

            String userPrompt = String.format("""
                根据以下岗位数据生成5种软技能的详细描述和证据。
                必须只生成这5种软技能：创新能力、学习能力、抗压能力、沟通能力、实习能力。
                每种软技能包含：name(中文名称)、score(0-100分，基于capabilityScores)、description(中文描述，针对该岗位具体说明)、evidence(2-3个中文具体例子)。
                
                岗位数据：
                %s
                
                返回JSON数组格式：
                [{"name":"创新能力","score":85,"description":"针对该岗位的具体描述","evidence":["例子1","例子2"]}]
                
                只返回JSON数组，不要markdown或其他文字。
                """, jobContext);

            String response = jobClassificationChatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();

            // Parse AI response
            if (response != null && !response.isBlank()) {
                // Clean response (remove markdown code blocks if present)
                String cleanedResponse = response.trim();
                if (cleanedResponse.startsWith("```")) {
                    cleanedResponse = cleanedResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
                }
                
                List<SoftSkillItemVO> softSkills = objectMapper.readValue(cleanedResponse, 
                        new TypeReference<List<SoftSkillItemVO>>() {});
                return softSkills != null ? softSkills : new ArrayList<>();
            }
        } catch (Exception e) {
            log.warn("AI soft skills generation failed for job {}: {}", job.getId(), e.getMessage());
        }
        
        // Fallback to basic soft skills based on capability scores
        return generateBasicSoftSkills(caps);
    }
    
    /**
     * Fallback: generate basic soft skills from capability scores
     * Only 5 types: innovation, learning, resilience, communication, internship
     */
    private List<SoftSkillItemVO> generateBasicSoftSkills(MarketCapabilityRequirementsVO caps) {
        List<SoftSkillItemVO> softSkills = new ArrayList<>();
        
        // Innovation - Innovation Ability
        if (caps.getInnovation() != null && caps.getInnovation() > 0) {
            SoftSkillItemVO innovation = new SoftSkillItemVO();
            innovation.setName("Innovation Ability");
            innovation.setScore(caps.getInnovation());
            innovation.setDescription("Ability to propose innovative solutions and optimize existing processes");
            innovation.setEvidence(Arrays.asList("Propose technical improvement solutions", "Participate in innovation projects"));
            softSkills.add(innovation);
        }
        
        // Learning - Learning Ability
        if (caps.getLearning() != null && caps.getLearning() > 0) {
            SoftSkillItemVO learning = new SoftSkillItemVO();
            learning.setName("Learning Ability");
            learning.setScore(caps.getLearning());
            learning.setDescription("Ability to quickly master new technologies and apply them to projects");
            learning.setEvidence(Arrays.asList("Learn new frameworks independently", "Complete technical courses"));
            softSkills.add(learning);
        }
        
        // Resilience - Resilience
        if (caps.getResilience() != null && caps.getResilience() > 0) {
            SoftSkillItemVO resilience = new SoftSkillItemVO();
            resilience.setName("Resilience");
            resilience.setScore(caps.getResilience());
            resilience.setDescription("Ability to maintain performance under pressure and tight deadlines");
            resilience.setEvidence(Arrays.asList("Deliver projects on schedule", "Handle multiple tasks effectively"));
            softSkills.add(resilience);
        }
        
        // Communication - Communication Skills
        if (caps.getCommunication() != null && caps.getCommunication() > 0) {
            SoftSkillItemVO communication = new SoftSkillItemVO();
            communication.setName("Communication Skills");
            communication.setScore(caps.getCommunication());
            communication.setDescription("Ability to collaborate with teams and express technical concepts clearly");
            communication.setEvidence(Arrays.asList("Participate in code reviews", "Write technical documentation"));
            softSkills.add(communication);
        }
        
        // Internship - Internship Ability
        if (caps.getInternship() != null && caps.getInternship() > 0) {
            SoftSkillItemVO internship = new SoftSkillItemVO();
            internship.setName("Internship Ability");
            internship.setScore(caps.getInternship());
            internship.setDescription("Ability to adapt to work environment and complete internship tasks");
            internship.setEvidence(Arrays.asList("Complete internship assignments", "Actively participate in team activities"));
            softSkills.add(internship);
        }
        
        return softSkills;
    }

    private MarketTrendsVO buildAggregatedTrends() {
        MarketTrendsVO trends = new MarketTrendsVO();
        trends.setJobProfileId(1L);
        trends.setJobName("Software Development Engineer");
        trends.setCity("Beijing");
        trends.setTimeRange("quarter");
        
        MarketSalaryTrendVO salary = new MarketSalaryTrendVO();
        MarketSalarySnapshotVO current = new MarketSalarySnapshotVO();
        current.setMin(new BigDecimal("15000"));
        current.setMax(new BigDecimal("35000"));
        current.setAvg(new BigDecimal("25000"));
        current.setCurrency("CNY");
        current.setUnit("month");
        
        MarketSalarySnapshotVO previous = new MarketSalarySnapshotVO();
        previous.setMin(new BigDecimal("14000"));
        previous.setMax(new BigDecimal("33000"));
        previous.setAvg(new BigDecimal("23500"));
        previous.setCurrency("CNY");
        previous.setUnit("month");
        
        salary.setCurrent(current);
        salary.setPrevious(previous);
        salary.setYoyGrowth(6.4);
        salary.setTrend("UP");
        trends.setSalary(salary);
        
        MarketDemandTrendVO demand = new MarketDemandTrendVO();
        demand.setLevel("HIGH");
        demand.setCurrentQuarter(1200);
        demand.setPreviousQuarter(1050);
        demand.setGrowthRate(14.3);
        demand.setTrend("UP");
        demand.setHistogram(Arrays.asList(800, 950, 1050, 1200));
        trends.setDemand(demand);
        
        List<MarketHotSkillVO> hotSkills = Arrays.asList(
                createHotSkill("Java", 450, 12.5),
                createHotSkill("Spring Boot", 380, 18.2),
                createHotSkill("MySQL", 320, 8.5),
                createHotSkill("Redis", 280, 22.1),
                createHotSkill("Microservices", 250, 35.7)
        );
        trends.setHotSkills(hotSkills);
        
        trends.setUpdatedAt(LocalDateTime.now().format(ISO_FORMATTER));
        return trends;
    }

    private MarketTrendsVO buildTrendsForJob(JobCategory job) {
        MarketTrendsVO trends = new MarketTrendsVO();
        trends.setJobProfileId(job.getId());
        trends.setJobName(job.getJobCategoryName());
        trends.setCity("Beijing");
        trends.setTimeRange("quarter");
        
        MarketSalaryTrendVO salary = new MarketSalaryTrendVO();
        MarketSalarySnapshotVO current = new MarketSalarySnapshotVO();
        current.setMin(job.getMinSalary() != null ? job.getMinSalary() : new BigDecimal("10000"));
        current.setMax(job.getMaxSalary() != null ? job.getMaxSalary() : new BigDecimal("25000"));
        if (current.getMin() != null && current.getMax() != null) {
            current.setAvg(current.getMin().add(current.getMax()).divide(new BigDecimal("2"), RoundingMode.HALF_UP));
        }
        current.setCurrency("CNY");
        current.setUnit("month");
        
        MarketSalarySnapshotVO previous = new MarketSalarySnapshotVO();
        previous.setMin(current.getMin().multiply(new BigDecimal("0.95")));
        previous.setMax(current.getMax().multiply(new BigDecimal("0.95")));
        previous.setAvg(current.getAvg().multiply(new BigDecimal("0.95")));
        previous.setCurrency("CNY");
        previous.setUnit("month");
        
        salary.setCurrent(current);
        salary.setPrevious(previous);
        salary.setYoyGrowth(5.0 + new Random().nextDouble() * 10);
        salary.setTrend("UP");
        trends.setSalary(salary);
        
        MarketDemandTrendVO demand = new MarketDemandTrendVO();
        demand.setLevel(calculateDemandLevel(job.getSourceJobCount()));
        demand.setCurrentQuarter(job.getSourceJobCount() != null ? job.getSourceJobCount() : 100);
        demand.setPreviousQuarter((int)((job.getSourceJobCount() != null ? job.getSourceJobCount() : 100) * 0.9));
        demand.setGrowthRate(10.0 + new Random().nextDouble() * 15);
        demand.setTrend("UP");
        demand.setHistogram(Arrays.asList(60, 80, 90, 100));
        trends.setDemand(demand);
        
        List<String> skills = parseJsonToList(job.getRequiredSkills());
        List<MarketHotSkillVO> hotSkills = skills.stream()
                .limit(5)
                .map(skill -> createHotSkill(skill, 50 + new Random().nextInt(200), 5 + new Random().nextDouble() * 30))
                .collect(Collectors.toList());
        trends.setHotSkills(hotSkills);
        
        trends.setUpdatedAt(LocalDateTime.now().format(ISO_FORMATTER));
        return trends;
    }

    private MarketInsightContentVO buildInsightContent(Long jobProfileId) {
        // Try to get real job data for AI insight generation
        JobCategory job = null;
        if (jobProfileId != null) {
            job = jobCategoryMapper.selectById(jobProfileId);
        }
        
        // If we have job data, try AI generation
        if (job != null) {
            try {
                return generateAiInsight(job);
            } catch (Exception e) {
                log.warn("AI insight generation failed for job {}: {}", job.getId(), e.getMessage());
            }
        }
        
        // Fallback to static content
        MarketInsightContentVO content = new MarketInsightContentVO();
        content.setTitle("市场洞察分析");
        content.setSummary("基于近期市场数据，该岗位展现出强劲的增长潜力，对专业人才的需求持续上升。企业正在积极招聘并提供具有竞争力的薪酬福利。");
        
        List<MarketSignalVO> signals = Arrays.asList(
                createSignal("平均薪资", "¥25K-35K/月", "UP"),
                createSignal("在招岗位", "1,200+ 职位", "UP"),
                createSignal("技能需求", "高增长", "UP"),
                createSignal("竞争程度", "中等水平", "STABLE")
        );
        content.setMarketSignals(signals);
        
        content.setIndustryTrends(Arrays.asList(
                "数字化转型推动需求增长",
                "云原生技能日益重要",
                "AI/ML集成成为标配",
                "远程工作机会持续扩展"
        ));
        
        List<MarketSuggestedActionVO> actions = Arrays.asList(
                createAction("强化核心技能", "聚焦热门技术领域", "HIGH"),
                createAction("获取专业认证", "行业认证提升竞争力", "MEDIUM"),
                createAction("打造作品集", "展示实战经验", "MEDIUM"),
                createAction("积极拓展人脉", "连接行业专业人士", "LOW")
        );
        content.setSuggestedActions(actions);
        
        return content;
    }
    
    /**
     * Generate market insight using AI
     */
    private MarketInsightContentVO generateAiInsight(JobCategory job) {
        String jobContext = String.format("""
            {
              "jobName": "%s",
              "jobLevel": "%s",
              "requiredSkills": %s,
              "description": "%s",
              "sourceJobCount": %d,
              "minSalary": %s,
              "maxSalary": %s
            }
            """,
            job.getJobCategoryName(),
            job.getJobLevel() != null ? job.getJobLevel() : "中级",
            job.getRequiredSkills() != null ? job.getRequiredSkills() : "[]",
            job.getJobDescription() != null ? job.getJobDescription().replace("\"", "\\\"").replace("\n", " ") : "",
            job.getSourceJobCount() != null ? job.getSourceJobCount() : 100,
            job.getMinSalary() != null ? job.getMinSalary().toString() : "15000",
            job.getMaxSalary() != null ? job.getMaxSalary().toString() : "35000"
        );
        
        String userPrompt = String.format("""
            您是位经验丰富的职业顾问。根据以下岗位数据生成市场洞察分析报告。
            
            岗位数据：
            %s
            
            请生成以下内容（全部使用中文）：
            1. summary: 2-3句话的市场洞察总结
            2. marketSignals: 4个市场信号（label中文名称、value具体数值或描述、trend趋势UP/DOWN/STABLE）
            3. industryTrends: 4个行业趋势（中文短语）
            4. suggestedActions: 4个建议行动（title中文标题、desc中文描述、priority优先级HIGH/MEDIUM/LOW）
            
            只返回JSON对象，不要markdown或其他文字。
            """, jobContext);
        
        String response = jobClassificationChatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
        
        if (response != null && !response.isBlank()) {
            // Clean response
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            }
            
            // Parse AI response
            try {
                MarketInsightContentVO content = objectMapper.readValue(cleanedResponse, MarketInsightContentVO.class);
                if (content != null) {
                    content.setTitle(job.getJobCategoryName() + " 市场洞察");
                    return content;
                }
            } catch (Exception parseEx) {
                log.warn("Failed to parse AI insight response: {}", parseEx.getMessage());
            }
        }
        
        throw new RuntimeException("AI response was empty or invalid");
    }

    // ==================== 工具方法 ====================

    private List<String> parseJsonToList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析JSON数组失败: {}", json, e);
            return new ArrayList<>();
        }
    }

    private String deriveIndustrySegment(String categoryCode) {
        if (categoryCode == null) return "技术";
        if (categoryCode.contains("JAVA") || categoryCode.contains("BACKEND")) return "后端开发";
        if (categoryCode.contains("FRONTEND") || categoryCode.contains("WEB")) return "前端开发";
        if (categoryCode.contains("DATA")) return "数据工程";
        if (categoryCode.contains("AI") || categoryCode.contains("ML")) return "AI/机器学习";
        if (categoryCode.contains("DEVOPS")) return "DevOps";
        if (categoryCode.contains("MOBILE")) return "移动开发";
        if (categoryCode.contains("TEST")) return "质量保障";
        return "技术";
    }

    private String calculateDemandLevel(Integer sourceJobCount) {
        if (sourceJobCount == null || sourceJobCount < 10) return "低";
        if (sourceJobCount < 30) return "中";
        if (sourceJobCount < 60) return "高";
        return "极高";
    }

    private MarketCapabilityRequirementsVO generateCapabilities(String jobLevel) {
        MarketCapabilityRequirementsVO caps = new MarketCapabilityRequirementsVO();
        int base = "SENIOR".equals(jobLevel) ? 70 : "MID".equals(jobLevel) ? 60 : 50;
        Random random = new Random();
        caps.setInnovation(base + random.nextInt(20));
        caps.setLearning(base + random.nextInt(20));
        caps.setResilience(base + random.nextInt(20));
        caps.setCommunication(base + random.nextInt(20));
        caps.setInternship(base + random.nextInt(20));
        return caps;
    }

    private List<String> generateCertificates(String categoryCode) {
        List<String> certs = new ArrayList<>();
        if (categoryCode != null) {
            if (categoryCode.contains("JAVA")) {
                certs.add("Oracle认证Java程序员");
                certs.add("Spring专业认证");
            } else if (categoryCode.contains("FRONTEND")) {
                certs.add("AWS认证开发者");
            } else if (categoryCode.contains("DATA")) {
                certs.add("AWS认证数据分析");
            }
        }
        certs.add("英语四级及以上");
        return certs;
    }

    private String determineTag(JobCategory job) {
        if (job.getSourceJobCount() != null && job.getSourceJobCount() > 50) return "热门";
        if (job.getMaxSalary() != null && job.getMaxSalary().compareTo(new BigDecimal("30000")) > 0) return "高薪";
        if ("INTERNSHIP".equals(job.getJobLevel())) return "入门级";
        return "推荐";
    }

    private List<String> generateHighlights(JobCategory job) {
        List<String> highlights = new ArrayList<>();
        if (job.getSourceJobCount() != null && job.getSourceJobCount() > 50) {
            highlights.add("需求旺盛");
        }
        if (job.getMaxSalary() != null && job.getMaxSalary().compareTo(new BigDecimal("30000")) > 0) {
            highlights.add("薪资竞争力强");
        }
        highlights.add("职业路径清晰");
        highlights.add("技能成长空间大");
        return highlights;
    }

    private Double calculateGrowthRate(JobCategory job) {
        // Simulated growth rate based on job characteristics
        double base = 5.0;
        if (job.getSourceJobCount() != null && job.getSourceJobCount() > 50) {
            base += 10.0;
        }
        if ("SENIOR".equals(job.getJobLevel())) {
            base += 5.0;
        }
        return base + new Random().nextDouble() * 10;
    }

    private String determineIcon(String categoryCode) {
        if (categoryCode == null) return "code";
        if (categoryCode.contains("JAVA")) return "java";
        if (categoryCode.contains("FRONTEND")) return "web";
        if (categoryCode.contains("DATA")) return "database";
        if (categoryCode.contains("AI")) return "brain";
        if (categoryCode.contains("DEVOPS")) return "server";
        return "code";
    }

    private String determineEducationRequirement(String jobLevel) {
        if ("SENIOR".equals(jobLevel)) return "本科及以上";
        if ("MID".equals(jobLevel)) return "本科优先";
        return "大专及以上";
    }

    private List<String> buildVerticalPath(JobCategory job) {
        List<String> path = new ArrayList<>();
        String level = job.getJobLevel();
        String name = job.getJobCategoryName();
        
        if ("INTERNSHIP".equals(level)) {
            path.add(name);
            path.add(name.replace("实习", "初级"));
            path.add(name.replace("实习", "中级"));
            path.add(name.replace("实习", "高级"));
        } else if ("JUNIOR".equals(level)) {
            path.add(name);
            path.add(name.replace("初级", "中级"));
            path.add(name.replace("初级", "高级"));
        } else {
            path.add(name);
            path.add("高级" + name);
            path.add(name + "主管");
            path.add(name + "经理");
        }
        return path;
    }

    private List<String> buildLateralPath(JobCategory job) {
        List<String> path = new ArrayList<>();
        String code = job.getJobCategoryCode();
        
        if (code != null) {
            if (code.contains("JAVA")) {
                path.add("Python开发工程师");
                path.add("Go开发工程师");
            } else if (code.contains("FRONTEND")) {
                path.add("全栈开发工程师");
                path.add("UI/UX设计师");
            } else if (code.contains("DATA")) {
                path.add("Data Scientist");
                path.add("ML Engineer");
            }
        }
        path.add("Technical Consultant");
        path.add("Product Manager");
        return path;
    }

    private MarketHotSkillVO createHotSkill(String skill, int demandCount, double growth) {
        MarketHotSkillVO hotSkill = new MarketHotSkillVO();
        hotSkill.setSkill(skill);
        hotSkill.setDemandCount(demandCount);
        hotSkill.setGrowth(growth);
        return hotSkill;
    }

    private MarketSignalVO createSignal(String label, String value, String trend) {
        MarketSignalVO signal = new MarketSignalVO();
        signal.setLabel(label);
        signal.setValue(value);
        signal.setTrend(trend);
        return signal;
    }

    private MarketSuggestedActionVO createAction(String title, String desc, String priority) {
        MarketSuggestedActionVO action = new MarketSuggestedActionVO();
        action.setTitle(title);
        action.setDesc(desc);
        action.setPriority(priority);
        return action;
    }
}
