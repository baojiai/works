package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.AdminService;
import com.course.aftersales.service.CustomerService;
import com.course.aftersales.service.EngineerApplicationService;
import com.course.aftersales.service.EngineerService;
import com.course.aftersales.service.NotificationService;
import com.course.aftersales.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class RoleControllerWebTest {
    @Test
    void wrongRolesCannotEnterAdminWarehouseOrEngineerPages() throws Exception {
        SessionUser customer = user("CUSTOMER");
        AdminService adminService = mock(AdminService.class);
        WarehouseService warehouseService = mock(WarehouseService.class);
        EngineerService engineerService = mock(EngineerService.class);

        mvc(new AdminController(adminService)).perform(get("/admin").sessionAttr("user", customer))
                .andExpect(status().isOk()).andExpect(view().name("error"))
                .andExpect(model().attribute("error", "当前账号无权执行此操作"));
        mvc(new WarehouseController(warehouseService)).perform(get("/warehouse/requests").sessionAttr("user", customer))
                .andExpect(status().isOk()).andExpect(view().name("error"))
                .andExpect(model().attribute("error", "当前账号无权执行此操作"));
        mvc(new EngineerController(engineerService)).perform(get("/engineer/profile").sessionAttr("user", customer))
                .andExpect(status().isOk()).andExpect(view().name("error"))
                .andExpect(model().attribute("error", "当前账号无权执行此操作"));

        verifyNoInteractions(adminService, warehouseService, engineerService);
    }

    @Test
    void authorizedRolesCanLoadTheirMainPages() throws Exception {
        AdminService adminService = mock(AdminService.class);
        when(adminService.data()).thenReturn(Collections.<String, Object>emptyMap());
        mvc(new AdminController(adminService)).perform(get("/admin").sessionAttr("user", user("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("admin/index"));
        verify(adminService).data();

        WarehouseService warehouseService = mock(WarehouseService.class);
        when(warehouseService.requestData()).thenReturn(Collections.<String, Object>emptyMap());
        mvc(new WarehouseController(warehouseService)).perform(
                        get("/warehouse/requests").sessionAttr("user", user("WAREHOUSE")))
                .andExpect(status().isOk()).andExpect(view().name("warehouse/requests"));
        verify(warehouseService).requestData();

        EngineerService engineerService = mock(EngineerService.class);
        when(engineerService.profileData(5L)).thenReturn(Collections.<String, Object>emptyMap());
        mvc(new EngineerController(engineerService)).perform(
                        get("/engineer/profile").sessionAttr("user", user("ENGINEER")))
                .andExpect(status().isOk()).andExpect(view().name("engineer/profile"));
        verify(engineerService).profileData(5L);
    }

    @Test
    void customerRepairAndEngineerApplicationPagesLoadForCustomer() throws Exception {
        SessionUser customer = user("CUSTOMER");
        CustomerService customerService = mock(CustomerService.class);
        when(customerService.formData()).thenReturn(
                Collections.<String, java.util.List<java.util.Map<String, Object>>>emptyMap());
        mvc(new CustomerController(customerService)).perform(
                        get("/customer/request").sessionAttr("user", customer))
                .andExpect(status().isOk()).andExpect(view().name("customer/request"));
        verify(customerService).formData();

        EngineerApplicationService applicationService = mock(EngineerApplicationService.class);
        when(applicationService.formData(5L)).thenReturn(Collections.<String, Object>emptyMap());
        mvc(new EngineerApplicationController(applicationService)).perform(
                        get("/engineer/apply").sessionAttr("user", customer))
                .andExpect(status().isOk()).andExpect(view().name("engineer/apply"));
        verify(applicationService).formData(5L);
    }

    @Test
    void notificationPageOnlyLoadsTheCurrentUsersMessages() throws Exception {
        NotificationService service = mock(NotificationService.class);
        when(service.list(5L)).thenReturn(Collections.<java.util.Map<String, Object>>emptyList());

        mvc(new NotificationController(service)).perform(
                        get("/notifications").sessionAttr("user", user("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attributeExists("notifications"));

        verify(service).list(5L);
    }

    private static MockMvc mvc(Object controller) {
        org.springframework.web.servlet.view.InternalResourceViewResolver resolver =
                new org.springframework.web.servlet.view.InternalResourceViewResolver("/WEB-INF/views/", ".jsp");
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(resolver).build();
    }

    private static SessionUser user(String role) {
        return new SessionUser(5L, role.toLowerCase(), "测试用户", role);
    }
}
