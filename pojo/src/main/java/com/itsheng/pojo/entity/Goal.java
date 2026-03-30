package com.itsheng.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Goal {
    private Long id;
    private Long userId;
    private String title;
    private String goalDesc;
    private String status;
    private Integer progress;
    private String eta;
    private Boolean isPrimary;
    private String successSalary;
    private String successCompanies;
    private String successCities;
    private String longTermAspirations;
    private String aiAdvice;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
