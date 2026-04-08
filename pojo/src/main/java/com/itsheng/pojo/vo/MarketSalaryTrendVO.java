package com.itsheng.pojo.vo;

import lombok.Data;

@Data
public class MarketSalaryTrendVO {
    private MarketSalarySnapshotVO current;
    private MarketSalarySnapshotVO previous;
    private Double yoyGrowth;
    private String trend;
}
