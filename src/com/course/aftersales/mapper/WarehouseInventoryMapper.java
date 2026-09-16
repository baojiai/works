package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface WarehouseInventoryMapper {
    List<Map<String,Object>> findRecentFlows();
    int lockInventory(@Param("partId") long partId, @Param("quantity") int quantity);
    int unlockInventory(@Param("partId") long partId, @Param("quantity") int quantity);
    int issueInventory(@Param("partId") long partId, @Param("quantity") int quantity);
    int adjustInventory(@Param("partId") long partId, @Param("quantity") int quantity);
    int insertRequestFlow(@Param("partId") long partId, @Param("orderId") Object orderId,
                          @Param("requestId") long requestId, @Param("flowType") String flowType,
                          @Param("quantity") int quantity, @Param("reason") String reason,
                          @Param("operatorId") long operatorId);
    int insertStockFlow(@Param("partId") long partId, @Param("flowType") String flowType,
                        @Param("quantity") int quantity, @Param("reason") String reason,
                        @Param("operatorId") long operatorId);
}
