package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketProfileListVO {
    private Long total;
    private Integer page;
    private Integer size;
    private List<MarketProfileItemVO> items;
    private String updatedAt;
}
