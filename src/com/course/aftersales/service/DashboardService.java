package com.course.aftersales.service;

import com.course.aftersales.mapper.DashboardMapper;
import com.course.aftersales.model.SessionUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DashboardService {
    private final DashboardMapper mapper;
    private final NotificationService notificationService;

    @Autowired
    public DashboardService(DashboardMapper mapper, NotificationService notificationService) {
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Map<String,Object> summary(SessionUser user) throws Exception {
        Map<String,Object> result = new LinkedHashMap<>();
        if ("ENGINEER".equals(user.getRole())) {
            result.put("我预约的服务", mapper.countEngineerCustomerAppointments(user.getId()));
            result.put("我收到的工单", mapper.countEngineerActiveOrders(user.getId()));
            result.put("可接单时段", mapper.countEngineerAvailableSchedules(user.getId()));
            result.put("未读通知", notificationService.unreadCount(user.getId()));
        } else if (user.hasRole("CUSTOMER")) {
            result.put("待服务预约", mapper.countCustomerActiveAppointments(user.getId()));
            result.put("待验收工单", mapper.countCustomerPendingAcceptance(user.getId()));
            result.put("已完成服务", mapper.countCustomerCompletedOrders(user.getId()));
            result.put("未读通知", notificationService.unreadCount(user.getId()));
        } else if (user.hasRole("WAREHOUSE")) {
            result.put("待审核申请", mapper.countPendingPartRequests());
            result.put("待出库申请", mapper.countApprovedPartRequests());
            result.put("库存预警", mapper.countInventoryWarnings());
            result.put("未读通知", notificationService.unreadCount(user.getId()));
        } else {
            result.put("启用用户", mapper.countActiveUsers());
            result.put("待改约异常", mapper.countPendingReschedules());
            result.put("进行中工单", mapper.countActiveOrders());
            result.put("未读通知", notificationService.unreadCount(user.getId()));
        }
        return result;
    }
}
