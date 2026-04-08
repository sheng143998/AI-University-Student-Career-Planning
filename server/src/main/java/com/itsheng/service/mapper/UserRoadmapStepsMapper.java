package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.UserRoadmapSteps;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户职业发展路径 Mapper 接口
 */
@Mapper
public interface UserRoadmapStepsMapper {

    /**
     * 插入用户职业发展路径
     * @param roadmapSteps 用户职业发展路径实体
     * @return 影响行数
     */
    int insert(UserRoadmapSteps roadmapSteps);

    /**
     * 根据用户 ID 查询职业发展路径
     * @param userId 用户 ID
     * @return 用户职业发展路径实体
     */
    UserRoadmapSteps selectByUserId(@Param("userId") Long userId);

    /**
     * 动态更新用户职业发展路径
     * @param roadmapSteps 用户职业发展路径实体
     * @return 影响行数
     */
    int update(UserRoadmapSteps roadmapSteps);
}
