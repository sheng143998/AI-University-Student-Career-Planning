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
     * 搜索职业节点（基于 job 表）
     * @param keyword 关键字（匹配岗位分类名称/编码）
     * @param limit 返回数量
     * @return 岗位分类列表
     */
    List<JobCategory> searchByKeyword(@Param("keyword") String keyword, @Param("limit") Integer limit);

    /**
     * 获取垂直晋升路径（同一 job_category_code 的不同 job_level）
     * @param categoryCode 类别编码
     * @return 岗位分类列表（按 INTERNSHIP -> JUNIOR -> MID -> SENIOR 排序）
     */
    List<JobCategory> selectVerticalPathByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * 随机获取一个类别编码（用于首页随机展示一条垂直晋升路径）
     * @return 类别编码
     */
    String selectRandomCategoryCode();

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
