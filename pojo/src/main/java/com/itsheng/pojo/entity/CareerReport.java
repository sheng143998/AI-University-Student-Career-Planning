package com.itsheng.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerReport {
    private Long id;
    private String reportNo;
    private Long userId;
    private Long studentCapabilityId;
    private Long targetJobProfileId;
    private Integer matchScore;
    private String matchDetails;
    private String selfDiscovery;
    private String targetJob;
    private String developmentPath;
    private String actionPlan;
    private String aiSuggestions;
    private String status;
    private Boolean isEditable;
    private String pdfFilePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
