package com.itsheng.service.service;

import com.itsheng.pojo.dto.GoalCreateDTO;
import com.itsheng.pojo.dto.GoalMilestoneCreateDTO;
import com.itsheng.pojo.dto.GoalMilestoneUpdateDTO;
import com.itsheng.pojo.dto.GoalUpdateDTO;
import com.itsheng.pojo.vo.GoalDetailVO;
import com.itsheng.pojo.vo.GoalsOverviewVO;
import com.itsheng.pojo.vo.IdVO;

public interface GoalsService {
    
    GoalsOverviewVO overview();
    
    IdVO createGoal(GoalCreateDTO dto);
    
    GoalDetailVO getGoalDetail(Long goalId);
    
    void updateGoal(Long goalId, GoalUpdateDTO dto);
    
    void deleteGoal(Long goalId);
    
    IdVO createMilestone(Long goalId, GoalMilestoneCreateDTO dto);
    
    void updateMilestone(Long milestoneId, GoalMilestoneUpdateDTO dto);
}
