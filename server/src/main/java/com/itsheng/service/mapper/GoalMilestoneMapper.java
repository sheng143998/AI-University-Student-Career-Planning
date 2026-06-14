package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.GoalMilestone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoalMilestoneMapper {
    
    List<GoalMilestone> findByGoalIdAndUserId(@Param("goalId") Long goalId, @Param("userId") Long userId);
    
    GoalMilestone findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    GoalMilestone findByGoalIdAndIdAndUserId(@Param("goalId") Long goalId, @Param("id") Long id, @Param("userId") Long userId);
    
    void insert(GoalMilestone milestone);
    
    int updateByIdAndUserId(GoalMilestone milestone);
    
    void deleteByGoalIdAndUserId(@Param("goalId") Long goalId, @Param("userId") Long userId);
}
