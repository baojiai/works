package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EngineerApplicationMapper {
    List<Map<String,Object>> findActiveAreas();
    List<Map<String,Object>> findActiveFaults();
    Map<String,Object> findLatestApplication(@Param("userId") long userId);
    Long findPendingApplicationId(@Param("userId") long userId);
    Long findActiveAreaId(@Param("areaId") long areaId);
    int insertApplication(Map<String,Object> application);
    Long findActiveFaultId(@Param("faultId") long faultId);
    int insertApplicationSkill(@Param("applicationId") long applicationId, @Param("faultId") long faultId);
    List<Long> findActiveAdminIds();
    int insertApplyNotification(@Param("adminId") long adminId, @Param("content") String content, @Param("applicationId") long applicationId);
    List<Map<String,Object>> findApplications();
    List<Map<String,Object>> findApplicationSkills();
    Map<String,Object> findPendingApplication(@Param("applicationId") long applicationId);
    int updateApplicationReview(@Param("applicationId") long applicationId, @Param("status") String status,
                                @Param("adminId") long adminId, @Param("comment") String comment);
    int updateUserAsEngineer(@Param("userId") long userId, @Param("realName") Object realName, @Param("phone") Object phone);
    Long findEngineerProfileId(@Param("userId") long userId);
    int insertEngineerProfile(@Param("userId") long userId, @Param("bio") Object bio);
    int updateEngineerProfile(@Param("userId") long userId, @Param("bio") Object bio);
    int deleteEngineerSkills(@Param("userId") long userId);
    List<Long> findApplicationFaultIds(@Param("applicationId") long applicationId);
    int insertEngineerSkill(@Param("userId") long userId, @Param("faultId") long faultId);
    int deleteEngineerAreas(@Param("userId") long userId);
    int insertEngineerArea(@Param("userId") long userId, @Param("areaId") Object areaId);
    int insertReviewNotification(@Param("userId") long userId, @Param("title") String title,
                                 @Param("content") String content, @Param("applicationId") long applicationId);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
