package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.UserCareerData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户职业数据 Mapper 接口
 */
@Mapper
public interface UserCareerDataMapper {

    /**
     * 插入用户职业数据
     * @param userCareerData 用户职业数据实体
     * @return 影响行数
     */
    int insert(UserCareerData userCareerData);

    /**
     * 根据用户 ID 查询职业数据
     * @param userId 用户 ID
     * @return 用户职业数据实体
     */
    UserCareerData selectByUserId(@Param("userId") Long userId);

    /**
     * 动态更新用户职业数据
     * @param userCareerData 用户职业数据实体
     * @return 影响行数
     */
    int update(UserCareerData userCareerData);
}
