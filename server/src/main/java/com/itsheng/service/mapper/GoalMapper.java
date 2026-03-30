package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.Goal;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GoalMapper {
    
    Goal findPrimaryByUserId(Long userId);
    
    List<Goal> findParallelByUserId(Long userId);
    
    Goal findByIdAndUserId(Long id, Long userId);
    
    void clearPrimaryByUserId(Long userId);
    
    void insert(Goal goal);
    
    void update(Goal goal);
    
    void deleteByIdAndUserId(Long id, Long userId);
}
