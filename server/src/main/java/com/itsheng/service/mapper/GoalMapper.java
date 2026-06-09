package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.Goal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoalMapper {
    
    Goal findPrimaryByUserId(@Param("userId") Long userId);
    
    List<Goal> findParallelByUserId(@Param("userId") Long userId);
    
    Goal findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    void clearPrimaryByUserId(@Param("userId") Long userId);
    
    void insert(Goal goal);
    
    int updateByIdAndUserId(Goal goal);

    int updateAiAdviceByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId, @Param("aiAdvice") String aiAdvice);
    
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
