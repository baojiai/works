package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.sql.Date;
import java.util.List;
import java.util.Map;

public interface EngineerScheduleMapper {
    List<Map<String,Object>> findActiveSlots();
    List<Map<String,Object>> findSchedules(@Param("engineerId") long engineerId);
    Long findEligibleEngineerId(@Param("engineerId") long engineerId);
    int insertSchedule(@Param("engineerId") long engineerId, @Param("serviceDate") Date serviceDate, @Param("slotId") long slotId);
    int closeSchedule(@Param("engineerId") long engineerId, @Param("scheduleId") long scheduleId);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
