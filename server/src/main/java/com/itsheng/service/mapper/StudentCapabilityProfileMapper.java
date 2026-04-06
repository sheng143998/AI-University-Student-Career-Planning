package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.StudentCapabilityProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学生能力画像 Mapper 接口
 */
@Mapper
public interface StudentCapabilityProfileMapper {

    /**
     * 插入能力画像记录
     * @param profile 能力画像
     * @return 影响行数
     */
    int insert(StudentCapabilityProfile profile);

    /**
     * 根据用户 ID 查询最新一条能力画像
     * @param userId 用户 ID
     * @return 能力画像记录
     */
    StudentCapabilityProfile selectByUserId(@Param("userId") Long userId);
}
