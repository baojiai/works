package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EngineerProfileMapper {
    Map<String,Object> findProfile(@Param("engineerId") long engineerId);
    List<Map<String,Object>> findFaultOptions(@Param("engineerId") long engineerId);
    List<Map<String,Object>> findAreaOptions(@Param("engineerId") long engineerId);
    int updateUserPhone(@Param("engineerId") long engineerId, @Param("phone") String phone);
    int updateProfileBio(@Param("engineerId") long engineerId, @Param("bio") String bio);
    int deleteSkills(@Param("engineerId") long engineerId);
    int insertSkill(@Param("engineerId") long engineerId, @Param("faultId") long faultId);
    int deleteAreas(@Param("engineerId") long engineerId);
    int insertArea(@Param("engineerId") long engineerId, @Param("areaId") long areaId);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
