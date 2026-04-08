package com.itsheng.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketSalaryRangeVO {
    private BigDecimal min;
    private BigDecimal max;
    private String currency;
    private String unit;
}
