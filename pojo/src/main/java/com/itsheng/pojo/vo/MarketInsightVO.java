package com.itsheng.pojo.vo;

import lombok.Data;

@Data
public class MarketInsightVO {
    private Long jobProfileId;
    private String jobName;
    private String city;
    private MarketInsightContentVO insight;
    private String updatedAt;
}
