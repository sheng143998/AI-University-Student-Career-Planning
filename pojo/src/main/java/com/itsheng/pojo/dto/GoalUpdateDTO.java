package com.itsheng.pojo.dto;

import com.itsheng.pojo.vo.AiAdviceVO;
import com.itsheng.pojo.vo.LongTermAspirationVO;
import com.itsheng.pojo.vo.SuccessCriteriaVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新目标请求")
public class GoalUpdateDTO {
    
    @Schema(description = "目标标题", example = "成为高级前端工程师")
    private String title;
    
    @Schema(description = "目标描述", example = "通过工程化与性能体系建设拿到更高级别岗位")
    private String desc;
    
    @Schema(description = "状态：TODO/IN_PROGRESS/DONE", example = "IN_PROGRESS")
    private String status;
    
    @Schema(description = "进度 0-100", example = "65")
    private Integer progress;
    
    @Schema(description = "预计达成时间", example = "2025年12月")
    private String eta;
    
    @Schema(description = "是否为主目标", example = "true")
    private Boolean isPrimary;
    
    @Schema(description = "成功准则")
    private SuccessCriteriaVO successCriteria;
    
    @Schema(description = "长期愿景列表")
    private List<LongTermAspirationVO> longTermAspirations;
    
    @Schema(description = "AI建议")
    private AiAdviceVO aiAdvice;
}
