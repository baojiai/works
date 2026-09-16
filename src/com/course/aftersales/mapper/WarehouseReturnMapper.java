package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Map;

public interface WarehouseReturnMapper {
    Map<String,Object> findReturnableItem(@Param("itemId") long itemId);
    int restoreInventory(@Param("partId") Object partId, @Param("quantity") int quantity);
    int incrementItemReturn(@Param("itemId") long itemId, @Param("quantity") int quantity);
    int insertReturnFlow(@Param("partId") Object partId, @Param("orderId") Object orderId,
                         @Param("requestId") Object requestId, @Param("quantity") int quantity,
                         @Param("reason") String reason, @Param("operatorId") long operatorId);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
