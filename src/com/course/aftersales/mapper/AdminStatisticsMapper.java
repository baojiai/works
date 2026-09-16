package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AdminStatisticsMapper {
    List<Map<String,Object>> findExceptions();
    List<Map<String,Object>> findRecentLogs();
    List<Map<String,Object>> findStatistics();
    List<Map<String,Object>> findPendingReschedulesBefore(@Param("threshold") LocalDateTime threshold);
    int markAppointmentExpired(@Param("appointmentId") long appointmentId);
    int cancelOrder(@Param("orderId") Object orderId);
    int insertRescheduleTimeoutChange(@Param("appointmentId") long appointmentId, @Param("adminId") long adminId);
    List<Map<String,Object>> findUpcomingAppointments(@Param("untilDate") LocalDate untilDate);
    List<Map<String,Object>> findOverdueOrders(@Param("threshold") LocalDateTime threshold);
    List<Long> findWarehouseManagerIds();
    List<Long> findOverduePartRequestIds(@Param("threshold") LocalDateTime threshold);
    Long findExistingNotification(@Param("receiverId") long receiverId, @Param("notificationType") String notificationType,
                                  @Param("businessType") String businessType, @Param("businessId") long businessId);
    int insertNotification(@Param("receiverId") long receiverId, @Param("notificationType") String notificationType,
                           @Param("title") String title, @Param("content") String content,
                           @Param("businessType") String businessType, @Param("businessId") long businessId);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
