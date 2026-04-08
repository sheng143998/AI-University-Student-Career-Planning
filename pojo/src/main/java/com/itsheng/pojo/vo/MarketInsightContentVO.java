package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketInsightContentVO {
    private String title;
    private String summary;
    private List<MarketSignalVO> marketSignals;
    private List<String> industryTrends;
    private List<MarketSuggestedActionVO> suggestedActions;
}
