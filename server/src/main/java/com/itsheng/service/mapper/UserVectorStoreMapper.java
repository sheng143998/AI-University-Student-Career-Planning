package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.UserVectorStore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserVectorStoreMapper {

    /**
     * 动态更新 user_vector_store 表记录
     * 只更新非 null 字段
     * @param userVectorStore 用户向量存储对象
     * @return 影响的行数
     */
    int update(UserVectorStore userVectorStore);

    /**
     * 根据 ID 查询向量存储记录
     * @param id 向量存储 ID
     * @return 向量存储对象
     */
    UserVectorStore selectByVectorStoreId(@Param("id") String id);
}
