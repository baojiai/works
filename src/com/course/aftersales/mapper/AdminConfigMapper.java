package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface AdminConfigMapper {
    List<Map<String,Object>> findConfigs();
    String findConfigValue(@Param("configKey") String configKey);
    int updateConfig(@Param("adminId") long adminId, @Param("configKey") String configKey,
                     @Param("configValue") String configValue);
    int insertConfig(@Param("adminId") long adminId, @Param("configKey") String configKey,
                     @Param("configValue") String configValue, @Param("description") String description);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
