package com.itsheng.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketSalarySnapshotVO {
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal avg;
    private String currency;
    private String unit;
}
