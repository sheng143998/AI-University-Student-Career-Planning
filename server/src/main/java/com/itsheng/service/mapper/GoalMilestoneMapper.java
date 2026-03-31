package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.GoalMilestone;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GoalMilestoneMapper {
    
    List<GoalMilestone> findByGoalId(Long goalId);
    
    GoalMilestone findByIdAndUserId(Long id, Long userId);
    
    void insert(GoalMilestone milestone);
    
    void update(GoalMilestone milestone);
    
    void deleteByGoalId(Long goalId);
}
