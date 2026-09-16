package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface AppointmentMapper {
    List<Map<String,Object>> findActiveSlots();
    List<Map<String,Object>> findCandidates(@Param("customerId") long customerId,
                                            @Param("requestId") long requestId,
                                            @Param("sort") String sort);
    Map<String,Object> findBookableMatch(@Param("customerId") long customerId,
                                         @Param("requestId") long requestId,
                                         @Param("engineerId") long engineerId,
                                         @Param("scheduleId") long scheduleId);
    int occupySchedule(@Param("scheduleId") long scheduleId);
    int insertAppointment(Map<String,Object> appointment);
    int insertRepairOrder(Map<String,Object> order);
    int insertCreatedOrderLog(@Param("orderId") long orderId, @Param("customerId") long customerId);
    int markRequestBooked(@Param("requestId") long requestId);
    int insertNewAppointmentNotification(@Param("engineerId") long engineerId, @Param("appointmentId") long appointmentId);
    Map<String,Object> findReplaceableAppointment(@Param("customerId") long customerId, @Param("appointmentId") long appointmentId);
    int markAppointmentRescheduled(@Param("appointmentId") long appointmentId);
    int releaseOccupiedSchedule(@Param("scheduleId") Object scheduleId);
    int cancelOrder(@Param("orderId") Object orderId);
    int insertRescheduleChange(@Param("appointmentId") long appointmentId, @Param("oldStatus") String oldStatus,
                               @Param("customerId") long customerId, @Param("newAppointmentId") long newAppointmentId);
    int insertRescheduleOrderLog(@Param("orderId") Object orderId, @Param("customerId") long customerId,
                                 @Param("oldStatus") Object oldStatus);
    List<Map<String,Object>> findAppointmentsForEngineer(@Param("userId") long userId);
    List<Map<String,Object>> findAppointmentsForCustomer(@Param("userId") long userId);
    List<Map<String,Object>> findAllAppointments();
    Map<String,Object> findCancelableAppointment(@Param("customerId") long customerId, @Param("appointmentId") long appointmentId);
    String findConfigValue(@Param("configKey") String configKey);
    int markAppointmentCancelled(@Param("appointmentId") long appointmentId, @Param("reason") String reason);
    int insertCancelChange(@Param("appointmentId") long appointmentId, @Param("customerId") long customerId,
                           @Param("reason") String reason);
    int insertCancelOrderLog(@Param("orderId") Object orderId, @Param("customerId") long customerId,
                             @Param("reason") String reason);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
