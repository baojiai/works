package com.course.aftersales.service;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.repository.Database;
import com.course.aftersales.util.Passwords;
import java.sql.Connection;
import java.util.Map;

public class AuthService {
    public SessionUser login(String account, String password) throws Exception {
        try (Connection c = Database.open()) {
            Map<String,Object> row = Database.one(c,
                "SELECT user_id,username,display_name,role_type,password_hash,status FROM system_user WHERE username=? OR phone=?", account, account);
            if (row == null || !"ACTIVE".equals(row.get("status"))) return null;
            if (!Passwords.sha256(password).equals(row.get("password_hash"))) return null;
            Database.update(c, "INSERT INTO operation_log(user_id,operation_type,business_type,business_id,description,created_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)",
                ((Number)row.get("user_id")).longValue(), "LOGIN", "USER", row.get("user_id"), "登录系统");
            return new SessionUser(((Number)row.get("user_id")).longValue(), String.valueOf(row.get("username")), String.valueOf(row.get("display_name")), String.valueOf(row.get("role_type")));
        }
    }

    public SessionUser registerCustomer(String phone, String displayName, String password, String confirmPassword) throws Exception {
        if (!phone.matches("^1[3-9]\\d{9}$")) throw new IllegalArgumentException("请输入正确的 11 位手机号");
        if (displayName == null || displayName.trim().isEmpty()) displayName = "用户" + phone.substring(7);
        if (password == null || password.length() < 6) throw new IllegalArgumentException("密码至少需要 6 位");
        if (!password.equals(confirmPassword)) throw new IllegalArgumentException("两次输入的密码不一致");
        final String name = displayName.trim();
        final long[] id = {0};
        Database.tx(c -> {
            Map<String,Object> exists = Database.one(c, "SELECT user_id FROM system_user WHERE phone=? OR username=?", phone, phone);
            if (exists != null) throw new IllegalStateException("该手机号已注册，请直接登录");
            id[0] = Database.insert(c,
                    "INSERT INTO system_user(username,password_hash,display_name,phone,role_type,status) VALUES(?,?,?,?, 'CUSTOMER','ACTIVE')",
                    phone, Passwords.sha256(password), name, phone);
            Database.update(c, "INSERT INTO customer_profile(customer_id,address) VALUES(?,?)", id[0], "");
            Database.update(c, "INSERT INTO operation_log(user_id,operation_type,business_type,business_id,description) VALUES(?,?,?,?,?)",
                    id[0], "REGISTER", "USER", id[0], "手机号注册客户账号");
        });
        return new SessionUser(id[0], phone, name, "CUSTOMER");
    }
}
