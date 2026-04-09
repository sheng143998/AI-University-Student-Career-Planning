package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.CareerReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CareerReportMapper {

    int insert(CareerReport report);

    CareerReport selectById(@Param("id") Long id);

    CareerReport selectLatestByUserId(@Param("userId") Long userId);

    List<CareerReport> selectByUserId(@Param("userId") Long userId);

    int update(CareerReport report);

    int deleteById(@Param("id") Long id);
}
