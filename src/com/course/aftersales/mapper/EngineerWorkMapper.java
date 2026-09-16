package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EngineerWorkMapper {
    Map<String,Object> findCancelableAppointment(@Param("engineerId") long engineerId, @Param("appointmentId") long appointmentId);
    int markAppointmentPendingReschedule(@Param("appointmentId") long appointmentId, @Param("reason") String reason);
    int releaseSchedule(@Param("scheduleId") Object scheduleId);
    int markOrderPendingReschedule(@Param("orderId") Object orderId);
    int insertEngineerCancelChange(@Param("appointmentId") long appointmentId, @Param("engineerId") long engineerId, @Param("reason") String reason);
    int insertPendingRescheduleLog(@Param("orderId") Object orderId, @Param("engineerId") long engineerId, @Param("reason") String reason);
    int insertRescheduleNotification(@Param("customerId") Object customerId, @Param("reason") String reason, @Param("appointmentId") long appointmentId);
    Long findRecordableOrderId(@Param("engineerId") long engineerId, @Param("orderId") long orderId);
    int insertRepairRecord(@Param("engineerId") long engineerId, @Param("orderId") long orderId,
                           @Param("diagnosis") String diagnosis, @Param("repairAction") String repairAction,
                           @Param("hours") double hours, @Param("remark") String remark);
    Map<String,Object> findOrderForTransition(@Param("engineerId") long engineerId, @Param("orderId") long orderId);
    long countRepairRecords(@Param("orderId") long orderId);
    long countOpenPartRequests(@Param("orderId") long orderId);
    int updateOrderStatus(@Param("orderId") long orderId, @Param("newStatus") String newStatus, @Param("action") String action);
    int insertOrderStatusLog(@Param("orderId") long orderId, @Param("engineerId") long engineerId,
                             @Param("oldStatus") String oldStatus, @Param("newStatus") String newStatus,
                             @Param("reason") String reason);
    int insertAcceptanceNotification(@Param("customerId") Object customerId, @Param("orderId") long orderId);
    List<Map<String,Object>> findAvailableParts();
    Long findPartRequestOrderId(@Param("engineerId") long engineerId, @Param("orderId") long orderId);
    long countPendingPartRequests(@Param("orderId") long orderId);
    int insertPartRequest(Map<String,Object> request);
    int insertPartRequestItem(@Param("requestId") long requestId, @Param("partId") long partId, @Param("quantity") int quantity);
    List<Long> findWarehouseManagerIds();
    int insertPartReviewNotification(@Param("managerId") long managerId, @Param("requestNo") String requestNo, @Param("requestId") long requestId);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
