package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "成功准则")
public class SuccessCriteriaVO {
    
    @Schema(description = "薪资预期", example = "¥30k - ¥45k / 月")
    private String salary;
    
    @Schema(description = "目标公司列表", example = "[\"腾讯\", \"字节跳动\", \"阿里巴巴\"]")
    private List<String> companies;
    
    @Schema(description = "目标城市列表", example = "[\"北京\", \"上海\", \"深圳\"]")
    private List<String> cities;
}
