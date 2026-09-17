package com.course.aftersales.service;

import com.course.aftersales.mapper.DashboardMapper;
import com.course.aftersales.model.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock
    private DashboardMapper mapper;
    @Mock
    private NotificationService notificationService;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(mapper, notificationService);
    }

    @Test
    void customerSummaryUsesCustomerCounters() throws Exception {
        SessionUser user = user("CUSTOMER");
        when(mapper.countCustomerActiveAppointments(8L)).thenReturn(2L);
        when(mapper.countCustomerPendingAcceptance(8L)).thenReturn(1L);
        when(mapper.countCustomerCompletedOrders(8L)).thenReturn(5L);
        when(notificationService.unreadCount(8L)).thenReturn(3L);

        Map<String, Object> summary = service.summary(user);

        assertEquals(2L, summary.get("待服务预约"));
        assertEquals(1L, summary.get("待验收工单"));
        assertEquals(5L, summary.get("已完成服务"));
        assertEquals(3L, summary.get("未读通知"));
    }

    @Test
    void engineerSummaryUsesEngineerCounters() throws Exception {
        SessionUser user = user("ENGINEER");
        when(mapper.countEngineerCustomerAppointments(8L)).thenReturn(1L);
        when(mapper.countEngineerActiveOrders(8L)).thenReturn(4L);
        when(mapper.countEngineerAvailableSchedules(8L)).thenReturn(6L);

        Map<String, Object> summary = service.summary(user);

        assertEquals(1L, summary.get("我预约的服务"));
        assertEquals(4L, summary.get("我收到的工单"));
        assertEquals(6L, summary.get("可接单时段"));
        verify(notificationService).unreadCount(8L);
    }

    @Test
    void warehouseSummaryUsesWarehouseCounters() throws Exception {
        SessionUser user = user("WAREHOUSE");
        when(mapper.countPendingPartRequests()).thenReturn(7L);
        when(mapper.countApprovedPartRequests()).thenReturn(2L);
        when(mapper.countInventoryWarnings()).thenReturn(1L);

        Map<String, Object> summary = service.summary(user);

        assertEquals(7L, summary.get("待审核申请"));
        assertEquals(2L, summary.get("待出库申请"));
        assertEquals(1L, summary.get("库存预警"));
    }

    @Test
    void adminSummaryUsesPlatformCounters() throws Exception {
        SessionUser user = user("ADMIN");
        when(mapper.countActiveUsers()).thenReturn(20L);
        when(mapper.countPendingReschedules()).thenReturn(2L);
        when(mapper.countActiveOrders()).thenReturn(9L);

        Map<String, Object> summary = service.summary(user);

        assertEquals(20L, summary.get("启用用户"));
        assertEquals(2L, summary.get("待改约异常"));
        assertEquals(9L, summary.get("进行中工单"));
    }

    private static SessionUser user(String role) {
        return new SessionUser(8L, "tester", "测试用户", role);
    }
}
