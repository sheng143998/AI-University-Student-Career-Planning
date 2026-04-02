package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.JobEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位数据 Mapper 接口
 */
@Mapper
public interface JobMapper {

    /**
     * 插入岗位数据
     * @param job 岗位实体
     * @return 影响行数
     */
    int insert(JobEntity job);

    /**
     * 批量插入岗位数据
     * @param jobs 岗位列表
     * @return 影响行数
     */
    int insertBatch(@Param("jobs") List<JobEntity> jobs);

    /**
     * 根据 ID 查询岗位数据
     * @param id 岗位 ID
     * @return 岗位实体
     */
    JobEntity selectById(@Param("id") Long id);

    /**
     * 按岗位名称分组，每组随机抽取指定数量的数据
     * @param sampleSize 每组抽样数量
     * @return 抽样后的岗位列表
     */
    List<JobEntity> selectSampledByPositionName(@Param("sampleSize") int sampleSize);

    /**
     * 查询所有岗位数据
     * @return 岗位列表
     */
    List<JobEntity> selectAll();

    /**
     * 按 ID 范围查询岗位数据
     * @param startId 起始 ID
     * @param endId 结束 ID
     * @return 岗位列表
     */
    List<JobEntity> selectByIdRange(@Param("startId") Long startId, @Param("endId") Long endId);

    /**
     * 动态更新岗位数据
     * @param job 岗位实体
     * @return 影响行数
     */
    int update(JobEntity job);
}
