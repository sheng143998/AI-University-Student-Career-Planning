package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.JobCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位分类 Mapper 接口
 */
@Mapper
public interface JobCategoryMapper {

    /**
     * 插入岗位分类记录
     * @param jobCategory 岗位分类实体
     * @return 影响行数
     */
    int insert(JobCategory jobCategory);

    /**
     * 批量插入岗位分类记录
     * @param jobCategories 岗位分类列表
     * @return 影响行数
     */
    int insertBatch(@Param("jobCategories") List<JobCategory> jobCategories);

    /**
     * 根据 ID 查询岗位分类
     * @param id 岗位分类 ID
     * @return 岗位分类实体
     */
    JobCategory selectById(@Param("id") Long id);

    /**
     * 根据类别编码和级别查询岗位分类
     * @param categoryCode 类别编码
     * @param level 级别
     * @return 岗位分类实体
     */
    JobCategory selectByCategoryAndLevel(
            @Param("categoryCode") String categoryCode,
            @Param("level") String level);

    /**
     * 查询所有岗位分类
     * @return 岗位分类列表
     */
    List<JobCategory> selectAll();

    /**
     * 根据类别编码查询所有级别
     * @param categoryCode 类别编码
     * @return 岗位分类列表
     */
    List<JobCategory> selectByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * 根据完整类别编码查询（编码已包含级别后缀）
     * @param fullCategoryCode 完整类别编码（如 JAVA_DEV_JUNIOR, BIO_RESEARCHER_MID）
     * @return 岗位分类实体
     */
    JobCategory selectByFullCategoryCode(@Param("fullCategoryCode") String fullCategoryCode);

    /**
     * 更新岗位分类记录
     * @param jobCategory 岗位分类实体
     * @return 影响行数
     */
    int update(JobCategory jobCategory);

    /**
     * 删除岗位分类记录
     * @param id 岗位分类 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
