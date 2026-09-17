package com.course.aftersales.service;

import com.course.aftersales.mapper.AuthMapper;
import com.course.aftersales.model.SessionUser;
import com.course.aftersales.util.Passwords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private AuthMapper mapper;

    @Test
    void activeUserCanLoginAndLoginIsAudited() throws Exception {
        when(mapper.findUserByAccount("13800000000")).thenReturn(userRow("ACTIVE", "secret123"));
        AuthService service = new AuthService(mapper);

        SessionUser user = service.login("13800000000", "secret123");

        assertEquals(9L, user.getId());
        assertEquals("CUSTOMER", user.getRole());
        verify(mapper).insertLoginLog(9L);
    }

    @Test
    void wrongPasswordOrDisabledAccountCannotLogin() throws Exception {
        when(mapper.findUserByAccount("wrong")).thenReturn(userRow("ACTIVE", "correct-password"));
        when(mapper.findUserByAccount("disabled")).thenReturn(userRow("DISABLED", "secret123"));
        AuthService service = new AuthService(mapper);

        assertNull(service.login("wrong", "incorrect-password"));
        assertNull(service.login("disabled", "secret123"));
        verify(mapper, never()).insertLoginLog(anyLong());
    }

    @Test
    void registrationRejectsInvalidInputAndDuplicatePhone() {
        AuthService service = new AuthService(mapper);

        assertEquals("请输入正确的 11 位手机号",
                assertThrows(IllegalArgumentException.class,
                        () -> service.registerCustomer("123", "张三", "123456", "123456")).getMessage());
        assertEquals("密码至少需要 6 位",
                assertThrows(IllegalArgumentException.class,
                        () -> service.registerCustomer("13800000000", "张三", "123", "123")).getMessage());
        assertEquals("两次输入的密码不一致",
                assertThrows(IllegalArgumentException.class,
                        () -> service.registerCustomer("13800000000", "张三", "123456", "654321")).getMessage());

        when(mapper.findExistingUserId("13800000000")).thenReturn(3L);
        assertEquals("该手机号已注册，请直接登录",
                assertThrows(IllegalStateException.class,
                        () -> service.registerCustomer("13800000000", "张三", "123456", "123456")).getMessage());
    }

    @Test
    void registrationCreatesUserProfileAndAuditLog() throws Exception {
        when(mapper.findExistingUserId("13800000000")).thenReturn(null);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> user = invocation.getArgument(0);
            user.put("userId", 21L);
            return 1;
        }).when(mapper).insertUser(any());
        AuthService service = new AuthService(mapper);

        SessionUser user = service.registerCustomer("13800000000", "  张三  ", "123456", "123456");

        assertEquals(21L, user.getId());
        assertEquals("张三", user.getDisplayName());
        ArgumentCaptor<Map<String, Object>> captor = mapCaptor();
        verify(mapper).insertUser(captor.capture());
        assertEquals("13800000000", captor.getValue().get("phone"));
        assertEquals(Passwords.sha256("123456"), captor.getValue().get("passwordHash"));
        verify(mapper).insertCustomerProfile(21L);
        verify(mapper).insertRegisterLog(21L);
    }

    @Test
    void blankDisplayNameGetsStableDefault() throws Exception {
        when(mapper.findExistingUserId("13800001234")).thenReturn(null);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> user = invocation.getArgument(0);
            user.put("userId", 22L);
            return 1;
        }).when(mapper).insertUser(any());
        AuthService service = new AuthService(mapper);

        SessionUser user = service.registerCustomer("13800001234", " ", "123456", "123456");

        assertEquals("用户1234", user.getDisplayName());
        assertTrue(user.hasRole("CUSTOMER"));
    }

    private static Map<String, Object> userRow(String status, String password) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("user_id", 9L);
        row.put("username", "13800000000");
        row.put("display_name", "测试客户");
        row.put("role_type", "CUSTOMER");
        row.put("status", status);
        row.put("password_hash", Passwords.sha256(password));
        return row;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }
}
