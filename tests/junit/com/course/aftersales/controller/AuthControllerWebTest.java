package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.AuthService;
import com.course.aftersales.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AuthControllerWebTest {
    @Mock
    private AuthService authService;
    @Mock
    private DashboardService dashboardService;
    private MockMvc authMvc;
    private MockMvc dashboardMvc;

    @BeforeEach
    void setUp() {
        authMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setViewResolvers(viewResolver()).build();
        dashboardMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new DashboardController(dashboardService))
                .setViewResolvers(viewResolver()).build();
    }

    @Test
    void loginPagesExposeTheCorrectEdition() throws Exception {
        authMvc.perform(get("/client/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("edition", "client"));

        authMvc.perform(get("/warehouse/login"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("edition", "warehouse"));

        authMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("edition", "admin"));
    }

    @Test
    void customerLoginStoresSessionAndRedirects() throws Exception {
        SessionUser user = user("CUSTOMER");
        when(authService.login("13800000000", "123456")).thenReturn(user);

        authMvc.perform(post("/client/login")
                        .param("account", "13800000000")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request()
                        .sessionAttribute("user", user));
    }

    @Test
    void editionRejectsAnAccountFromAnotherRole() throws Exception {
        when(authService.login("customer", "123456")).thenReturn(user("CUSTOMER"));

        authMvc.perform(post("/admin/login")
                        .param("account", "customer")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("edition", "admin"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void invalidLoginAndRegistrationErrorsStayOnTheirForms() throws Exception {
        when(authService.login("missing", "bad-password")).thenReturn(null);
        authMvc.perform(post("/client/login")
                        .param("account", "missing")
                        .param("password", "bad-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"));

        when(authService.registerCustomer("123", "测试", "123456", "123456"))
                .thenThrow(new IllegalArgumentException("请输入正确的 11 位手机号"));
        authMvc.perform(post("/register")
                        .param("phone", "123")
                        .param("displayName", "测试")
                        .param("password", "123456")
                        .param("confirmPassword", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "请输入正确的 11 位手机号"));
    }

    @Test
    void dashboardRequiresSessionAndLoadsSummaryForLoggedInUser() throws Exception {
        dashboardMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        SessionUser user = user("CUSTOMER");
        when(dashboardService.summary(user)).thenReturn(Collections.<String, Object>singletonMap("未读通知", 2L));
        dashboardMvc.perform(get("/dashboard").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("summary"));
        verify(dashboardService).summary(user);
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user("CUSTOMER"));

        authMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/login"));
    }

    private static SessionUser user(String role) {
        return new SessionUser(7L, "tester", "测试用户", role);
    }

    private static org.springframework.web.servlet.view.InternalResourceViewResolver viewResolver() {
        return new org.springframework.web.servlet.view.InternalResourceViewResolver("/WEB-INF/views/", ".jsp");
    }
}
