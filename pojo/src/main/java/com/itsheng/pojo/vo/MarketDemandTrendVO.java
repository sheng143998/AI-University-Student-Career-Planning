package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketDemandTrendVO {
    private String level;
    private Integer currentQuarter;
    private Integer previousQuarter;
    private Double growthRate;
    private String trend;
    private List<Integer> histogram;
}
