package com.itsheng.pojo.vo;

import lombok.Data;
import java.util.List;

/**
 * 软技能详情（带描述和证据）
 */
@Data
public class SoftSkillItemVO {
    private String name;
    private Integer score;
    private String description;
    private List<String> evidence;
}
