package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

public interface AdminAuditMapper {
    int updateQualification(@Param("engineerId") long engineerId, @Param("status") String status);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
