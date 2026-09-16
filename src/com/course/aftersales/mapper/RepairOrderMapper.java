package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface RepairOrderMapper {
    List<Map<String,Object>> findOrdersForEngineer(@Param("userId") long userId);
    List<Map<String,Object>> findOrdersForCustomer(@Param("userId") long userId);
    List<Map<String,Object>> findAllOrders();
    Map<String,Object> findOrderDetailForEngineer(@Param("orderId") long orderId, @Param("userId") long userId);
    Map<String,Object> findOrderDetailForCustomer(@Param("orderId") long orderId, @Param("userId") long userId);
    Map<String,Object> findOrderDetail(@Param("orderId") long orderId);
    List<Map<String,Object>> findRepairRecords(@Param("orderId") long orderId);
    List<Map<String,Object>> findStatusLogs(@Param("orderId") long orderId);
    List<Map<String,Object>> findPartUsage(@Param("orderId") long orderId);
    List<Map<String,Object>> findAcceptances(@Param("orderId") long orderId);
    Map<String,Object> findReview(@Param("orderId") long orderId);
}
