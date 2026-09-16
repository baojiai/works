package com.course.aftersales.service;

import com.course.aftersales.mapper.AuthMapper;
import com.course.aftersales.model.SessionUser;
import com.course.aftersales.util.Passwords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {
    private final AuthMapper mapper;

    @Autowired
    public AuthService(AuthMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public SessionUser login(String account, String password) throws Exception {
        Map<String,Object> row = mapper.findUserByAccount(account);
        if (row == null || !"ACTIVE".equals(row.get("status"))) return null;
        if (!Passwords.sha256(password).equals(row.get("password_hash"))) return null;
        long userId = ((Number) row.get("user_id")).longValue();
        mapper.insertLoginLog(userId);
        return new SessionUser(userId, String.valueOf(row.get("username")), String.valueOf(row.get("display_name")), String.valueOf(row.get("role_type")));
    }

    @Transactional(rollbackFor = Exception.class)
    public SessionUser registerCustomer(String phone, String displayName, String password, String confirmPassword) throws Exception {
        if (!phone.matches("^1[3-9]\\d{9}$")) throw new IllegalArgumentException("请输入正确的 11 位手机号");
        if (displayName == null || displayName.trim().isEmpty()) displayName = "用户" + phone.substring(7);
        if (password == null || password.length() < 6) throw new IllegalArgumentException("密码至少需要 6 位");
        if (!password.equals(confirmPassword)) throw new IllegalArgumentException("两次输入的密码不一致");
        final String name = displayName.trim();
        Long existingId = mapper.findExistingUserId(phone);
        if (existingId != null) throw new IllegalStateException("该手机号已注册，请直接登录");

        Map<String,Object> user = new LinkedHashMap<>();
        user.put("phone", phone);
        user.put("passwordHash", Passwords.sha256(password));
        user.put("displayName", name);
        mapper.insertUser(user);

        long userId = ((Number) user.get("userId")).longValue();
        mapper.insertCustomerProfile(userId);
        mapper.insertRegisterLog(userId);
        return new SessionUser(userId, phone, name, "CUSTOMER");
    }
}
