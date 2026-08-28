package com.course.aftersales.service;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.repository.Database;
import java.sql.Connection;
import java.util.*;

public class DashboardService {
    public Map<String,Object> summary(SessionUser user) throws Exception {
        Map<String,Object> result = new LinkedHashMap<>();
        try (Connection c = Database.open()) {
            if (user.hasRole("CUSTOMER")) {
                result.put("待服务预约", count(c,"SELECT COUNT(*) c FROM appointment WHERE customer_id=? AND status IN ('BOOKED','PENDING_RESCHEDULE')",user.getId()));
                result.put("待验收工单", count(c,"SELECT COUNT(*) c FROM repair_order WHERE customer_id=? AND order_status='PENDING_ACCEPTANCE'",user.getId()));
                result.put("已完成服务", count(c,"SELECT COUNT(*) c FROM repair_order WHERE customer_id=? AND order_status='COMPLETED'",user.getId()));
            } else if (user.hasRole("ENGINEER")) {
                result.put("未来空闲时段", count(c,"SELECT COUNT(*) c FROM engineer_schedule WHERE engineer_id=? AND status='AVAILABLE' AND service_date>=CURRENT_DATE",user.getId()));
                result.put("待处理工单", count(c,"SELECT COUNT(*) c FROM repair_order WHERE engineer_id=? AND order_status IN ('PENDING_VISIT','REPAIRING','WAITING_PARTS','REWORK')",user.getId()));
                result.put("待审核配件", count(c,"SELECT COUNT(*) c FROM part_request WHERE engineer_id=? AND status='PENDING'",user.getId()));
            } else if (user.hasRole("WAREHOUSE")) {
                result.put("待审核申请", count(c,"SELECT COUNT(*) c FROM part_request WHERE status='PENDING'"));
                result.put("待出库申请", count(c,"SELECT COUNT(*) c FROM part_request WHERE status='APPROVED'"));
                result.put("库存预警", count(c,"SELECT COUNT(*) c FROM part_inventory WHERE available_quantity<=warning_threshold"));
            } else {
                result.put("启用用户", count(c,"SELECT COUNT(*) c FROM system_user WHERE status='ACTIVE'"));
                result.put("待改约异常", count(c,"SELECT COUNT(*) c FROM appointment WHERE status='PENDING_RESCHEDULE'"));
                result.put("进行中工单", count(c,"SELECT COUNT(*) c FROM repair_order WHERE order_status NOT IN ('COMPLETED','CANCELLED')"));
            }
            result.put("未读通知", count(c,"SELECT COUNT(*) c FROM notification WHERE receiver_id=? AND is_read=FALSE",user.getId()));
        }
        return result;
    }
    private long count(Connection c, String sql, Object... args) throws Exception {
        Map<String,Object> row = Database.one(c,sql,args);
        return ((Number)row.get("c")).longValue();
    }
}

