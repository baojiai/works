package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface WarehouseRequestMapper {
    List<Map<String,Object>> findRequests();
    Map<String,Object> findPendingRequest(@Param("requestId") long requestId);
    Map<String,Object> findApprovedRequest(@Param("requestId") long requestId);
    List<Map<String,Object>> findRequestItems(@Param("requestId") long requestId);
    int approveRequest(@Param("requestId") long requestId, @Param("operatorId") long operatorId,
                       @Param("comment") String comment);
    int rejectRequest(@Param("requestId") long requestId, @Param("operatorId") long operatorId,
                      @Param("comment") String comment);
    int cancelRequest(@Param("requestId") long requestId, @Param("reason") String reason);
    int markRequestIssued(@Param("requestId") long requestId);
    int markRequestCompleted(@Param("requestId") long requestId);
    int incrementItemIssued(@Param("itemId") Object itemId, @Param("quantity") int quantity);
    int insertNotification(@Param("receiverId") long receiverId, @Param("notificationType") String notificationType,
                           @Param("title") String title, @Param("content") String content,
                           @Param("businessType") String businessType, @Param("businessId") long businessId);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
