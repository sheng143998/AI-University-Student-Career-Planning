package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketHotJobsVO {
    private List<MarketHotJobItemVO> items;
    private String updatedAt;
}
