package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.JobVectorStore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位向量存储 Mapper 接口
 */
@Mapper
public interface JobVectorStoreMapper {

    /**
     * 插入岗位向量数据
     * @param jobVectorStore 岗位向量存储实体
     * @return 影响行数
     */
    int insert(JobVectorStore jobVectorStore);

    /**
     * 批量插入岗位向量数据
     * @param jobVectorStores 岗位向量存储实体列表
     * @return 影响行数
     */
    int insertBatch(@Param("stores") List<JobVectorStore> jobVectorStores);

    /**
     * 根据岗位 ID 查询向量数据
     * @param jobId 岗位 ID
     * @return 岗位向量存储实体
     */
    JobVectorStore selectByJobId(@Param("jobId") Long jobId);

    /**
     * 根据向量存储 ID 查询向量数据
     * @param id 向量存储 ID
     * @return 岗位向量存储实体
     */
    JobVectorStore selectByVectorStoreId(@Param("id") String id);

    /**
     * 动态更新岗位向量数据
     * @param jobVectorStore 岗位向量存储实体
     * @return 影响行数
     */
    int update(JobVectorStore jobVectorStore);

    /**
     * 根据向量搜索相似岗位
     * @param vector embedding 向量
     * @param limit 返回数量
     * @return 相似的岗位向量列表
     */
    List<JobVectorStore> searchByVector(@Param("vector") String vector, @Param("limit") Integer limit);

    /**
     * 批量更新岗位向量数据的 job_id 和 content_hash 字段
     * @param jobVectorStores 岗位向量存储实体列表（包含 id, jobId, contentHash）
     * @return 影响行数
     */
    int updateBatch(@Param("stores") List<JobVectorStore> jobVectorStores);
}
