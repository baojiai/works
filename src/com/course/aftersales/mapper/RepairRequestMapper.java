package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface RepairRequestMapper {
    List<Map<String,Object>> findActiveDevices();
    List<Map<String,Object>> findActiveFaults();
    List<Map<String,Object>> findActiveAreas();
    Long findMatchingActiveFault(@Param("faultId") long faultId, @Param("deviceId") long deviceId);
    int insertRepairRequest(Map<String,Object> request);
    Map<String,Object> findRequest(@Param("customerId") long customerId, @Param("requestId") long requestId);
}
