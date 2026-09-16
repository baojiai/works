package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface AdminBasicDataMapper {
    List<Map<String,Object>> findDevices();
    List<Map<String,Object>> findFaults();
    List<Map<String,Object>> findAreas();
    List<Map<String,Object>> findSlots();
    int insertDevice(Map<String,Object> data);
    int insertFault(Map<String,Object> data);
    int insertArea(Map<String,Object> data);
    int insertSlot(Map<String,Object> data);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
